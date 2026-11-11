import { readFile } from 'node:fs/promises';
import { defineConfig } from 'vite';

// The browser build ships the compiled engine byte for byte (no re-minification, so its
// SHA-256 matches the CI artifact), the exact TeaVM notices and the project licence.
const notices = {
  'engine.js': './src/generated/engine.js',
  'licenses/TeaVM-LICENSE.txt': '../docs/licenses/TeaVM-LICENSE.txt',
  'licenses/TeaVM-NOTICE.txt': '../docs/licenses/TeaVM-NOTICE.txt',
  'licenses/LICENSE.txt': '../LICENSE',
};

function shipNotices() {
  return {
    name: 'ship-notices',
    apply: 'build',
    async generateBundle() {
      for (const [fileName, source] of Object.entries(notices)) {
        this.emitFile({ type: 'asset', fileName, source: await readFile(new URL(source, import.meta.url)) });
      }
    },
  };
}

export default defineConfig({ base: './', build: { target: 'es2022' }, plugins: [shipNotices()] });
