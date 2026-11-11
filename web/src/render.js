// Canvas renderer. It draws a snapshot of the Java engine and never decides anything:
// positions, lanes, refuges and states all come from the snapshot.
const COLS = 13;
const ROWS = 13;
const HOME_CENTRES = [2.5, 6.5, 10.5];
const WATER_ROWS = [0, 1, 2, 4, 5, 6];
const BANK_ROWS = [3, 7, 12];
const ROAD_ROWS = [8, 9, 10, 11];
const ANGLE = { UP: 0, RIGHT: Math.PI / 2, DOWN: Math.PI, LEFT: -Math.PI / 2 };

function readPalette() {
  const style = getComputedStyle(document.documentElement);
  const get = name => style.getPropertyValue(name).trim();
  return {
    paper: get('--paper'),
    paper2: get('--paper-2'),
    ink: get('--ink'),
    ink3: get('--ink-3'),
    accent: get('--frog'),
    bank: get('--board-bank'),
    road: get('--board-road'),
    roadLine: get('--board-road-line'),
    water: get('--board-water'),
    waterLine: get('--board-water-line'),
    log: get('--board-log'),
    logLine: get('--board-log-line'),
    car: get('--board-car'),
    glass: get('--board-car-glass'),
    pad: get('--board-pad'),
  };
}

function roundedRect(ctx, x, y, w, h, r) {
  const radius = Math.max(0, Math.min(r, w / 2, h / 2));
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.arcTo(x + w, y, x + w, y + h, radius);
  ctx.arcTo(x + w, y + h, x, y + h, radius);
  ctx.arcTo(x, y + h, x, y, radius);
  ctx.arcTo(x, y, x + w, y, radius);
  ctx.closePath();
}

export function createRenderer(canvas) {
  const ctx = canvas.getContext('2d');
  let palette = readPalette();
  let cell = 0;
  let scale = 1;
  let background = null;

  function resize() {
    const rect = canvas.getBoundingClientRect();
    scale = Math.min(window.devicePixelRatio || 1, 3);
    const pixels = Math.max(1, Math.round(rect.width * scale));
    if (canvas.width !== pixels || canvas.height !== pixels) {
      canvas.width = pixels;
      canvas.height = pixels;
    }
    cell = pixels / COLS;
    background = null;
  }

  function refresh() {
    palette = readPalette();
    background = null;
  }

  // Static rows are painted once per size and theme.
  function paintBackground() {
    const layer = document.createElement('canvas');
    layer.width = canvas.width;
    layer.height = canvas.height;
    const g = layer.getContext('2d');
    const px = 1 * scale;
    for (const row of WATER_ROWS) {
      g.fillStyle = palette.water;
      g.fillRect(0, row * cell, layer.width, cell);
      g.strokeStyle = palette.waterLine;
      g.lineWidth = 1.5 * px;
      g.lineCap = 'round';
      for (let i = 0; i < 5; i++) {
        const x = ((i * 2.7 + row * 1.9) % COLS) * cell;
        const y = (row + (i % 2 ? 0.32 : 0.7)) * cell;
        g.beginPath();
        g.moveTo(x, y);
        g.quadraticCurveTo(x + cell * 0.25, y - cell * 0.09, x + cell * 0.5, y);
        g.quadraticCurveTo(x + cell * 0.75, y + cell * 0.09, x + cell, y);
        g.stroke();
      }
    }
    for (const row of BANK_ROWS) {
      g.fillStyle = palette.bank;
      g.fillRect(0, row * cell, layer.width, cell);
      g.strokeStyle = palette.accent;
      g.globalAlpha = 0.28;
      g.lineWidth = 1.2 * px;
      for (let i = 0; i < 18; i++) {
        const x = ((i * 0.77 + row * 0.41) % COLS) * cell;
        const y = (row + 0.25 + ((i * 37) % 50) / 100) * cell;
        g.beginPath();
        g.moveTo(x, y + cell * 0.08);
        g.lineTo(x + cell * 0.04, y - cell * 0.04);
        g.moveTo(x + cell * 0.09, y + cell * 0.08);
        g.lineTo(x + cell * 0.1, y - cell * 0.02);
        g.stroke();
      }
      g.globalAlpha = 1;
    }
    g.fillStyle = palette.road;
    g.fillRect(0, ROAD_ROWS[0] * cell, layer.width, ROAD_ROWS.length * cell);
    g.strokeStyle = palette.roadLine;
    g.lineWidth = 2 * px;
    g.setLineDash([cell * 0.42, cell * 0.38]);
    for (const row of ROAD_ROWS.slice(1)) {
      g.beginPath();
      g.moveTo(0, row * cell);
      g.lineTo(layer.width, row * cell);
      g.stroke();
    }
    g.setLineDash([]);
    g.lineWidth = 1.5 * px;
    for (const y of [ROAD_ROWS[0] * cell + px, (ROAD_ROWS.at(-1) + 1) * cell - px]) {
      g.beginPath();
      g.moveTo(0, y);
      g.lineTo(layer.width, y);
      g.stroke();
    }
    background = layer;
  }

  function drawLog(x, row, length) {
    const top = (row + 0.17) * cell;
    const h = cell * 0.66;
    roundedRect(ctx, x * cell, top, length * cell, h, cell * 0.24);
    ctx.fillStyle = palette.log;
    ctx.fill();
    ctx.strokeStyle = palette.logLine;
    ctx.lineWidth = 1.5 * scale;
    ctx.stroke();
    ctx.beginPath();
    for (const [dy, from, to] of [[0.36, 0.35, 0.55], [0.64, 0.12, 0.42]]) {
      const y = top + h * dy;
      ctx.moveTo((x + length * from) * cell, y);
      ctx.lineTo((x + length * to) * cell, y);
    }
    ctx.stroke();
  }

  function drawVehicle(x, row, length, speed) {
    const top = (row + 0.2) * cell;
    const h = cell * 0.6;
    const left = x * cell;
    const w = length * cell;
    const forward = speed >= 0;
    const truck = length > 1.7;
    ctx.fillStyle = palette.car;
    if (truck) {
      const cab = cell * 0.62;
      const gap = cell * 0.06;
      const cabX = forward ? left + w - cab : left;
      const boxX = forward ? left : left + cab + gap;
      roundedRect(ctx, boxX, top, w - cab - gap, h, cell * 0.08);
      ctx.fill();
      roundedRect(ctx, cabX, top + h * 0.06, cab, h * 0.88, cell * 0.14);
      ctx.fill();
      ctx.fillStyle = palette.glass;
      const glassX = forward ? cabX + cab - cell * 0.24 : cabX + cell * 0.08;
      roundedRect(ctx, glassX, top + h * 0.2, cell * 0.16, h * 0.6, cell * 0.04);
      ctx.fill();
      return;
    }
    roundedRect(ctx, left, top, w, h, cell * 0.16);
    ctx.fill();
    ctx.fillStyle = palette.glass;
    const front = forward ? left + w - cell * 0.46 : left + cell * 0.24;
    const back = forward ? left + cell * 0.14 : left + w - cell * 0.3;
    roundedRect(ctx, front, top + h * 0.16, cell * 0.22, h * 0.68, cell * 0.05);
    ctx.fill();
    roundedRect(ctx, back, top + h * 0.22, cell * 0.16, h * 0.56, cell * 0.04);
    ctx.fill();
  }

  // Frog seen from above, facing up before rotation. Unit = one cell.
  function drawFrog(cx, cy, direction, { size = 1, alpha = 1, squash = 0 } = {}) {
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.translate(cx, cy);
    ctx.rotate(ANGLE[direction] ?? 0);
    const s = cell * size;
    ctx.scale(1 - squash * 0.5, 1 + squash);
    ctx.strokeStyle = palette.accent;
    ctx.fillStyle = palette.accent;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.lineWidth = s * 0.075;
    ctx.beginPath();
    for (const side of [-1, 1]) {
      ctx.moveTo(side * s * 0.14, -s * 0.1);
      ctx.lineTo(side * s * 0.3, -s * 0.2);
      ctx.lineTo(side * s * 0.28, -s * 0.33);
      ctx.moveTo(side * s * 0.16, s * 0.14);
      ctx.lineTo(side * s * 0.34, s * 0.24);
      ctx.lineTo(side * s * 0.24, s * 0.38);
    }
    ctx.stroke();
    ctx.beginPath();
    ctx.ellipse(0, s * 0.02, s * 0.22, s * 0.27, 0, 0, Math.PI * 2);
    ctx.fill();
    for (const side of [-1, 1]) {
      ctx.beginPath();
      ctx.fillStyle = palette.paper;
      ctx.arc(side * s * 0.1, -s * 0.17, s * 0.075, 0, Math.PI * 2);
      ctx.fill();
      ctx.beginPath();
      ctx.fillStyle = palette.ink;
      ctx.arc(side * s * 0.1, -s * 0.19, s * 0.036, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }

  function drawPad(index, filled) {
    const cx = HOME_CENTRES[index] * cell;
    const cy = cell * 0.5;
    const r = cell * 0.4;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.arc(cx, cy, r, -Math.PI * 0.38, Math.PI * 1.46);
    ctx.closePath();
    ctx.fillStyle = palette.pad;
    ctx.fill();
    ctx.strokeStyle = palette.accent;
    ctx.lineWidth = 2 * scale;
    ctx.stroke();
    if (filled) drawFrog(cx, cy, 'UP', { size: 0.78 });
  }

  function drawHitMark(snapshot, effect, now) {
    const x = Math.min(COLS - 0.3, Math.max(0.3, snapshot.frog.x)) * cell;
    const y = (snapshot.frog.y + 0.5) * cell;
    const cause = snapshot.event.type;
    if (cause === 'WATER' || cause === 'EDGE') {
      const progress = effect.animate ? Math.min(1, (now - effect.since) / 700) : 1;
      ctx.strokeStyle = palette.ink3;
      ctx.lineWidth = 1.5 * scale;
      for (const k of [0.45, 0.75, 1]) {
        ctx.globalAlpha = 0.25 + 0.6 * (1 - k * progress * 0.6);
        ctx.beginPath();
        ctx.arc(x, y, cell * 0.42 * k * (0.35 + 0.65 * progress), 0, Math.PI * 2);
        ctx.stroke();
      }
      ctx.globalAlpha = 1;
      return;
    }
    drawFrog(x, y, snapshot.frog.direction, { alpha: 0.35 });
    ctx.strokeStyle = palette.ink;
    ctx.lineWidth = 2.2 * scale;
    ctx.lineCap = 'round';
    ctx.beginPath();
    if (cause === 'TIME') {
      ctx.arc(x, y, cell * 0.26, 0, Math.PI * 2);
      ctx.moveTo(x, y);
      ctx.lineTo(x, y - cell * 0.17);
      ctx.moveTo(x, y);
      ctx.lineTo(x + cell * 0.12, y);
    } else {
      const d = cell * 0.2;
      ctx.moveTo(x - d, y - d);
      ctx.lineTo(x + d, y + d);
      ctx.moveTo(x + d, y - d);
      ctx.lineTo(x - d, y + d);
    }
    ctx.stroke();
  }

  function draw(snapshot, effect, now) {
    if (!cell) resize();
    if (!background) paintBackground();
    ctx.drawImage(background, 0, 0);
    if (!snapshot) return;
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, canvas.width, canvas.height);
    ctx.clip();
    for (const lane of snapshot.lanes) {
      for (const x of lane.positions) {
        if (lane.type === 'RIVER') drawLog(x, lane.row, lane.length);
        else drawVehicle(x, lane.row, lane.length, lane.speed);
      }
    }
    snapshot.homes.forEach((filled, i) => drawPad(i, filled));
    const state = snapshot.state;
    if (state === 'HIT' || state === 'LOST') {
      if (['CAR', 'WATER', 'EDGE', 'TIME'].includes(snapshot.event.type)) drawHitMark(snapshot, effect, now);
    } else if (snapshot.frog.y > 0) {
      drawFrog(snapshot.frog.x * cell, (snapshot.frog.y + 0.5) * cell, snapshot.frog.direction, { squash: effect.squash });
    }
    ctx.restore();
  }

  return { draw, resize, refresh, rows: ROWS };
}
