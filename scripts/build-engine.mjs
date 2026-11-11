import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const windows = process.platform === 'win32';
const wrapper = join(root, windows ? 'mvnw.cmd' : 'mvnw');
const result = spawnSync(wrapper, ['-B', '-Pweb', 'verify'], {
  cwd: root, stdio: 'inherit', shell: windows,
});
if (result.error) console.error('Compilation du moteur impossible. Java 17 et un accès aux dépôts Maven sont nécessaires.', result.error.message);
process.exit(result.status ?? 1);
