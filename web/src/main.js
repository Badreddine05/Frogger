import './tokens.css';
import './style.css';
import { setupThemeButton } from './theme.js';
import { createRenderer } from './render.js';

const $ = selector => document.querySelector(selector);
const canvas = $('#board');
const overlay = $('#overlay');
const gameSection = $('#jeu');
const ui = {
  kicker: $('#panel-kicker'),
  title: $('#panel-title'),
  text: $('#panel-text'),
  modes: $('#modes'),
  primary: $('#panel-primary'),
  secondary: $('#panel-secondary'),
  score: $('#score'),
  record: $('#record'),
  modeName: $('#mode-name'),
  pips: $('#lives-pips'),
  livesText: $('#lives-text'),
  level: $('#level'),
  saved: $('#saved'),
  timeText: $('#time-text'),
  timeMeter: $('#time-meter'),
  timeFill: $('#time-fill'),
  message: $('#message'),
  pause: $('#pause-button'),
  restart: $('#new-button'),
  seed: $('#seed'),
  rules: $('#rules-dialog'),
  confirm: $('#confirm-dialog'),
};

const MODE_NAMES = { WALK: 'Balade', CLASSIC: 'Classique', EXPRESS: 'Express' };
const ENGAGED = new Set(['PLAYING', 'PAUSED', 'HIT', 'HOME', 'LEVEL_CLEAR']);
const HIT_TITLES = {
  CAR: 'Percutée par un véhicule',
  WATER: "Tombée à l'eau",
  EDGE: 'Emportée hors du terrain',
  TIME: 'Temps écoulé',
};
const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
const number = new Intl.NumberFormat('fr-FR');
const plural = (n, one, many) => `${n} ${n > 1 ? many : one}`;

/* Records, one per difficulty, kept on this device only. ------------------ */
const RECORD_KEY = 'frogger-records-v1';
function loadRecords() {
  try {
    const raw = JSON.parse(localStorage.getItem(RECORD_KEY) || '{}');
    const clean = {};
    for (const mode of Object.keys(MODE_NAMES)) {
      const value = Number(raw?.[mode]);
      clean[mode] = Number.isSafeInteger(value) && value > 0 ? value : 0;
    }
    return clean;
  } catch {
    return { WALK: 0, CLASSIC: 0, EXPRESS: 0 };
  }
}
function saveRecords() {
  try {
    localStorage.setItem(RECORD_KEY, JSON.stringify(records));
  } catch {
    /* Private browsing or full storage: records last for this visit. */
  }
}
const records = loadRecords();
let recordAtStart = 0;

/* Engine ------------------------------------------------------------------ */
let engine = null;
let snap = null;
let lastEventId = -1;
let lastAnnounced = { text: '', at: 0 };
let shownState = null;
let accumulated = 0;
let lastFrame = 0;
let hopAt = -1;
let effect = { since: 0, animate: false };
let userEngaged = false;
const renderer = createRenderer(canvas);

const read = () => (snap = JSON.parse(engine.snapshot()));
const randomSeed = () => crypto.getRandomValues(new Int32Array(1))[0];
const selectedMode = () => ui.modes.querySelector('input:checked')?.value ?? 'CLASSIC';

function startGame(mode) {
  userEngaged = true;
  engine.start(mode, randomSeed());
  read();
  recordAtStart = records[mode];
  accumulated = 0;
  lastFrame = performance.now();
  update();
  focusBoard();
}

function send(name) {
  userEngaged = true;
  engine.command(name);
  read();
  if (snap.state === 'PLAYING') {
    accumulated = 0;
    lastFrame = performance.now();
  }
  update();
}

function move(direction) {
  if (!snap || snap.state !== 'PLAYING') return false;
  const { x, y } = snap.frog;
  engine.command(direction);
  read();
  if (snap.frog.x !== x || snap.frog.y !== y) hopAt = performance.now();
  update();
  return true;
}

function pauseIfPlaying() {
  if (snap?.state === 'PLAYING') send('pause');
}

// Keep the board in view and, on phones where the pad sits below it, the pad too.
function focusBoard() {
  canvas.focus({ preventScroll: true });
  const board = canvas.getBoundingClientRect();
  const pad = document.querySelector('.pad').getBoundingClientRect();
  const bottom = pad.top > board.bottom ? pad.bottom : board.bottom;
  let delta = 0;
  if (board.top < 0) delta = board.top - 12;
  else if (bottom > innerHeight) delta = Math.min(board.top - 12, bottom - innerHeight + 12);
  if (delta) scrollBy({ top: delta, behavior: reducedMotion.matches ? 'auto' : 'smooth' });
}

/* Loop: real elapsed time, fractions kept, at most 100 ms per frame. ------- */
function frame(now) {
  if (snap?.state === 'PLAYING') {
    accumulated += Math.min(Math.max(0, now - lastFrame), 100);
    const step = Math.floor(accumulated);
    if (step > 0) {
      accumulated -= step;
      engine.advance(step);
      read();
      update();
    }
  }
  lastFrame = now;
  const hop = reducedMotion.matches || hopAt < 0 ? 1 : (now - hopAt) / 140;
  const squash = hop < 1 ? Math.sin(hop * Math.PI) * 0.14 : 0;
  renderer.draw(snap, { ...effect, squash }, now);
  requestAnimationFrame(frame);
}

/* DOM state --------------------------------------------------------------- */
const cache = {};
function setText(key, element, value) {
  if (cache[key] === value) return;
  cache[key] = value;
  element.textContent = value;
}

function renderLives(lives, total) {
  const key = `${lives}/${total}`;
  if (cache.lives === key) return;
  cache.lives = key;
  ui.pips.replaceChildren(...Array.from({ length: total }, (_, i) => {
    const pip = document.createElement('i');
    if (i < lives) pip.className = 'on';
    return pip;
  }));
  ui.livesText.textContent = plural(lives, 'vie', 'vies');
}

function update() {
  if (!snap) return;
  const mode = snap.difficulty;
  if (ENGAGED.has(snap.state) || snap.state === 'WON' || snap.state === 'LOST') {
    if (snap.score > records[mode]) records[mode] = snap.score;
  }
  setText('score', ui.score, number.format(snap.score));
  setText('mode', ui.modeName, MODE_NAMES[mode]);
  setText('record', ui.record, records[mode] ? number.format(records[mode]) : 'aucun');
  const maxLives = mode === 'WALK' ? 5 : 3;
  renderLives(snap.lives, maxLives);
  setText('level', ui.level, `${snap.level} sur 3`);
  setText('saved', ui.saved, `${snap.saved} sur 9`);
  const seconds = Math.ceil(snap.remainingMs / 1000);
  const limit = Math.round(snap.timeLimitMs / 1000);
  setText('time', ui.timeText, `${seconds} s`);
  if (cache.meter !== `${seconds}/${limit}`) {
    cache.meter = `${seconds}/${limit}`;
    ui.timeMeter.setAttribute('aria-valuemax', String(limit));
    ui.timeMeter.setAttribute('aria-valuenow', String(seconds));
    ui.timeMeter.setAttribute('aria-valuetext', `${seconds} secondes sur ${limit}`);
    ui.timeText.classList.toggle('is-low', snap.state === 'PLAYING' && seconds <= 10);
  }
  ui.timeFill.style.transform = `scaleX(${Math.max(0, Math.min(1, snap.remainingMs / snap.timeLimitMs))})`;
  setText('seed', ui.seed, snap.state === 'MENU' && !userEngaged ? 'aucune' : String(snap.seed));

  if (snap.event.id !== lastEventId) {
    lastEventId = snap.event.id;
    announce(snap.event.message, snap.event.type);
    if (['CAR', 'WATER', 'EDGE', 'TIME'].includes(snap.event.type)) {
      effect = { since: performance.now(), animate: !reducedMotion.matches };
    }
  }
  if (snap.state !== shownState) {
    const previous = shownState;
    shownState = snap.state;
    if (snap.state === 'WON' || snap.state === 'LOST' || snap.state === 'MENU') saveRecords();
    showPanel(previous);
  }
}

function announce(text, type) {
  const now = performance.now();
  // A repeated refusal (blocked jump) is only announced once every two seconds.
  if (type === 'BLOCKED' && lastAnnounced.text === text && now - lastAnnounced.at < 2000) return;
  lastAnnounced = { text, at: now };
  ui.message.textContent = text;
}

/* Panel shown above the board for every state except PLAYING. ------------- */
function panelContent() {
  const s = snap;
  const mode = MODE_NAMES[s.difficulty];
  switch (s.state) {
    case 'MENU':
      return { kicker: 'Nouvelle partie', title: 'Choisir un rythme', text: 'Trois étapes, de trois refuges chacune.', modes: true, primary: ['Commencer', () => startGame(selectedMode())] };
    case 'PAUSED':
      return { kicker: 'Pause', title: 'Partie en pause', text: 'Le temps et le trafic sont arrêtés.', primary: ['Reprendre', () => { send('resume'); focusBoard(); }], secondary: ['Abandonner', askAbandon] };
    case 'HIT':
      return { kicker: `Vie perdue · ${plural(s.lives, 'vie restante', 'vies restantes')}`, loss: true, title: HIT_TITLES[s.event.type] ?? 'Vie perdue', text: s.event.message, primary: ['Repartir', () => { send('continue'); focusBoard(); }] };
    case 'HOME': {
      const filled = s.homes.filter(Boolean).length;
      return { kicker: 'Refuge atteint', title: 'Une grenouille à l’abri', text: `${plural(filled, 'refuge occupé', 'refuges occupés')} sur 3 pour cette étape.`, primary: ['Traversée suivante', () => { send('continue'); focusBoard(); }] };
    }
    case 'LEVEL_CLEAR':
      return { kicker: `Étape ${s.level} terminée`, title: 'Trois refuges occupés', text: 'Bonus de 500 points. À l’étape suivante, trafic et courant accélèrent.', primary: ['Étape suivante', () => { send('continue'); focusBoard(); }] };
    case 'WON':
      return { kicker: `Partie gagnée · ${mode}`, title: 'Neuf grenouilles à l’abri', text: finalLine(s), primary: ['Rejouer', () => startGame(s.difficulty)], secondary: ['Changer de rythme', () => { send('menu'); }] };
    case 'LOST':
      return { kicker: `Partie terminée · ${mode}`, loss: true, title: 'Plus de vies', text: `${HIT_TITLES[s.event.type] ?? 'Vie perdue'} à l’étape ${s.level}. ${finalLine(s)}`, primary: ['Rejouer', () => startGame(s.difficulty)], secondary: ['Changer de rythme', () => { send('menu'); }] };
    default:
      return null;
  }
}

function finalLine(s) {
  const beat = s.score > recordAtStart && s.score > 0;
  return `Score final : ${number.format(s.score)}${beat ? ', nouveau record sur cet appareil' : ''}.`;
}

let primaryAction = null;
let secondaryAction = null;
function showPanel(previous) {
  const content = panelContent();
  ui.pause.disabled = !(snap.state === 'PLAYING' || snap.state === 'PAUSED');
  ui.pause.textContent = snap.state === 'PAUSED' ? 'Reprendre' : 'Pause';
  if (!content) {
    overlay.hidden = true;
    primaryAction = secondaryAction = null;
    if (overlay.contains(document.activeElement) || document.activeElement === document.body) focusBoard();
    return;
  }
  ui.kicker.textContent = content.kicker;
  ui.kicker.classList.toggle('is-loss', Boolean(content.loss));
  ui.title.textContent = content.title;
  ui.text.textContent = content.text ?? '';
  ui.modes.hidden = !content.modes;
  if (content.modes) {
    for (const mode of Object.keys(MODE_NAMES)) {
      const slot = ui.modes.querySelector(`[data-record="${mode}"]`);
      slot.textContent = records[mode] ? `record ${number.format(records[mode])}` : '';
    }
    const radio = ui.modes.querySelector(`input[value="${snap.difficulty}"]`);
    if (radio && previous) radio.checked = true;
  }
  [ui.primary.textContent, primaryAction] = content.primary;
  ui.secondary.hidden = !content.secondary;
  if (content.secondary) [ui.secondary.textContent, secondaryAction] = content.secondary;
  overlay.hidden = false;
  // Move focus to the panel once the player has interacted, never on first load.
  if (userEngaged && !document.querySelector('dialog[open]')) {
    const target = content.modes ? ui.modes.querySelector('input:checked') : ui.primary;
    target?.focus({ preventScroll: true });
  }
}

ui.primary.addEventListener('click', () => primaryAction?.());
ui.secondary.addEventListener('click', () => secondaryAction?.());

/* Dialogs ----------------------------------------------------------------- */
let dialogReturn = null;
function openDialog(dialog, opener) {
  pauseIfPlaying();
  dialogReturn = opener ?? document.activeElement;
  dialog.showModal();
}
for (const dialog of [ui.rules, ui.confirm]) {
  dialog.addEventListener('close', () => {
    const target = dialogReturn && document.contains(dialogReturn) && !dialogReturn.closest('[hidden]') ? dialogReturn : overlay.hidden ? canvas : ui.primary;
    target.focus({ preventScroll: true });
    dialogReturn = null;
  });
  dialog.addEventListener('click', event => {
    if (event.target === dialog || event.target.closest('[data-close]')) dialog.close();
  });
}
function askAbandon() {
  openDialog(ui.confirm, ui.restart);
}
$('#rules-button').addEventListener('click', event => openDialog(ui.rules, event.currentTarget));
$('#confirm-abandon').addEventListener('click', () => {
  dialogReturn = null;
  ui.confirm.close();
  send('menu');
});
ui.restart.addEventListener('click', () => {
  if (ENGAGED.has(snap.state)) askAbandon();
  else send('menu');
});
ui.pause.addEventListener('click', () => {
  if (snap.state === 'PLAYING') send('pause');
  else if (snap.state === 'PAUSED') {
    send('resume');
    focusBoard();
  }
});

/* Keyboard ---------------------------------------------------------------- */
const ARROWS = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right' };
const LETTERS = { w: 'up', z: 'up', s: 'down', a: 'left', q: 'left', d: 'right' };
addEventListener('keydown', event => {
  if (!snap || event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
  if (document.querySelector('dialog[open]')) return;
  const target = event.target instanceof Element ? event.target : document.body;
  const field = target.closest('input, select, textarea, [contenteditable="true"]');
  const direction = ARROWS[event.key] ?? (event.key.length === 1 ? LETTERS[event.key.toLowerCase()] : undefined);
  if (direction) {
    if (field || snap.state !== 'PLAYING') return;
    if (target !== document.body && !gameSection.contains(target)) return;
    event.preventDefault();
    move(direction);
    return;
  }
  if (event.key === 'p' || event.key === 'P' || event.key === 'Escape') {
    if (event.repeat) return;
    if (snap.state === 'PLAYING') {
      event.preventDefault();
      send('pause');
    } else if (snap.state === 'PAUSED') {
      event.preventDefault();
      send('resume');
      focusBoard();
    }
    return;
  }
  if (event.key === 'Enter' && !event.repeat && !overlay.hidden && primaryAction) {
    if (target.closest('button, a, summary')) return;
    event.preventDefault();
    primaryAction();
  }
});

/* Touch and mouse direction pad: one press, one jump; holding repeats. ----- */
for (const button of document.querySelectorAll('.pad button')) {
  const direction = button.dataset.dir;
  let timer = 0;
  let repeats = 0;
  const stop = () => {
    clearTimeout(timer);
    button.classList.remove('is-down');
  };
  const repeat = () => {
    if (++repeats > 40 || snap?.state !== 'PLAYING') return stop();
    move(direction);
    timer = setTimeout(repeat, 150);
  };
  button.addEventListener('pointerdown', event => {
    if (event.button !== 0) return;
    event.preventDefault();
    try {
      button.setPointerCapture(event.pointerId);
    } catch {
      /* Pointer already released: the press still counts as one jump. */
    }
    button.classList.add('is-down');
    repeats = 0;
    move(direction);
    clearTimeout(timer);
    timer = setTimeout(repeat, 320);
  });
  for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) button.addEventListener(type, stop);
  // Keyboard activation of a focused pad button (Enter or Space) sends one jump.
  button.addEventListener('click', event => {
    if (event.detail === 0) move(direction);
  });
  button.addEventListener('contextmenu', event => event.preventDefault());
}

/* Pause when the page is hidden or the window loses focus; resume is explicit. */
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    pauseIfPlaying();
    saveRecords();
  }
});
addEventListener('blur', pauseIfPlaying);
addEventListener('pagehide', saveRecords);

/* Size and theme ------------------------------------------------------------ */
new ResizeObserver(() => renderer.resize()).observe(canvas);
setupThemeButton($('#theme-button'), () => requestAnimationFrame(() => renderer.refresh()));

/* Boot ---------------------------------------------------------------------- */
try {
  // Development serves the generated module directly; the build ships it untouched as ../engine.js.
  const engineUrl = import.meta.env.DEV ? './generated/engine.js' : new URL(/* @vite-ignore */ '../engine.js', import.meta.url).href;
  engine = await import(/* @vite-ignore */ engineUrl);
  read();
  update();
  requestAnimationFrame(now => {
    lastFrame = now;
    frame(now);
  });
} catch (error) {
  console.error(error);
  $('#engine-error').hidden = false;
  overlay.hidden = true;
  for (const control of document.querySelectorAll('.pad button, #pause-button, #new-button')) control.disabled = true;
  renderer.draw(null, {}, 0);
}
