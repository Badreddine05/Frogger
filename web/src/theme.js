// Theme toggle shared with the portfolio and its applications.
// Absent key = follow the system; "light" | "dark" = forced.
const KEY = 'bc-theme';
const NAMES = { auto: 'automatique', light: 'clair', dark: 'sombre' };
const SHORT = { auto: 'Auto', light: 'Clair', dark: 'Sombre' };
const NEXT = { auto: 'light', light: 'dark', dark: 'auto' };

function readTheme() {
  try {
    const value = localStorage.getItem(KEY);
    return value === 'light' || value === 'dark' ? value : 'auto';
  } catch {
    return 'auto';
  }
}

function storeTheme(value) {
  try {
    if (value === 'auto') localStorage.removeItem(KEY);
    else localStorage.setItem(KEY, value);
  } catch {
    /* Storage unavailable: the choice lasts for this page only. */
  }
}

function applyTheme(value) {
  const root = document.documentElement;
  if (value === 'auto') root.removeAttribute('data-theme');
  else root.setAttribute('data-theme', value);
  for (const meta of document.querySelectorAll('meta[name="theme-color"]')) {
    const scheme = meta.getAttribute('data-scheme');
    meta.setAttribute('media', value === 'auto' ? `(prefers-color-scheme: ${scheme})` : scheme === value ? 'all' : 'not all');
  }
}

export function setupThemeButton(button, onChange) {
  let theme = readTheme();
  const render = () => {
    button.dataset.mode = theme;
    button.querySelector('.theme-label').textContent = SHORT[theme];
    button.setAttribute('aria-label', `Thème ${NAMES[theme]}. Passer au thème ${NAMES[NEXT[theme]]}.`);
    button.title = `Thème : ${NAMES[theme]}`;
  };
  applyTheme(theme);
  render();
  button.addEventListener('click', () => {
    theme = NEXT[theme];
    storeTheme(theme);
    applyTheme(theme);
    render();
    onChange();
  });
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (theme === 'auto') onChange();
  });
}
