import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { delimiter, join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as engine from '../src/generated/engine.js';

const root = join(dirname(fileURLToPath(import.meta.url)), '../..');
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
function almostEqual(actual, expected, path = 'snapshot') {
  if (typeof expected === 'number') {
    assert.ok(Math.abs(actual - expected) <= 1e-8, `${path}: ${actual} != ${expected}`);
  } else if (Array.isArray(expected)) {
    assert.equal(actual.length, expected.length, `${path}.length`);
    expected.forEach((value, i) => almostEqual(actual[i], value, `${path}[${i}]`));
  } else if (expected && typeof expected === 'object') {
    assert.deepEqual(Object.keys(actual).sort(), Object.keys(expected).sort(), path);
    for (const key of Object.keys(expected)) almostEqual(actual[key], expected[key], `${path}.${key}`);
  } else assert.equal(actual, expected, path);
}
function run(action) {
  const parts = action.split(':');
  if (parts[0] === 'start') engine.start(parts[1], Number(parts[2]));
  else if (parts[0] === 'tick') engine.advance(Number(parts[1]));
  else engine.command(action);
  return JSON.parse(engine.snapshot());
}

test('TeaVM reproduces JVM snapshots across 463 real inputs and all difficulties', () => {
  const actions = [];
  for (const [mode, seed] of [['CLASSIC', 2021], ['WALK', -2147483648], ['EXPRESS', 2147483647]]) {
    actions.push(`start:${mode}:${seed}`, 'left', 'down', 'tick:17', 'tick:103', 'up', 'pause', 'tick:2000', 'right', 'resume');
    let random = 12345;
    for (let i = 0; i < 130; i++) {
      random = (Math.imul(random, 1664525) + 1013904223) | 0;
      actions.push(i % 4 === 0 ? `tick:${(random >>> 1) % 350}` : ['up', 'left', 'right', 'down', 'continue'][(random >>> 1) % 5]);
    }
    // Consecutive timeouts, explicitly continued, must reach HIT then LOST and freeze.
    actions.push('tick:60000', 'tick:60000', 'continue', 'tick:60000', 'tick:60000', 'continue', 'tick:60000', 'tick:60000', 'continue', 'tick:60000', 'tick:60000', 'continue', 'tick:60000', 'menu');
  }
  actions.push('start:CLASSIC:2021');
  const jvm = spawnSync(java, ['-Dfile.encoding=UTF-8', '-cp', [join(root, 'target/classes'), join(root, 'target/test-classes')].join(delimiter), 'frogger.core.ParityTrace', ...actions], { encoding: 'utf8', maxBuffer: 10_000_000 });
  assert.equal(jvm.status, 0, jvm.stderr || jvm.error?.message);
  const expected = jvm.stdout.trim().split(/\r?\n/).map(JSON.parse);
  assert.equal(expected.length, actions.length);
  actions.forEach((action, i) => almostEqual(run(action), expected[i], `${i}:${action}`));
});

test('compiled browser engine does not tick when receiving directions', () => {
  engine.start('CLASSIC', 7); const before = JSON.parse(engine.snapshot());
  engine.command('left'); engine.command('left'); engine.command('down');
  const after = JSON.parse(engine.snapshot());
  assert.deepEqual(after.lanes, before.lanes); assert.equal(after.elapsedMs, 0);
  assert.equal(after.frog.direction, 'LEFT'); assert.equal(after.frog.x, 5.5);
});

test('compiled browser engine freezes paused state and resets on explicit start', () => {
  engine.start('WALK', 10); engine.advance(500); engine.command('pause');
  const paused = engine.snapshot(); engine.advance(5000); engine.command('up');
  assert.equal(engine.snapshot(), paused);
  engine.command('resume'); engine.advance(20); assert.equal(JSON.parse(engine.snapshot()).elapsedMs, 520);
  engine.start('EXPRESS', 11); const fresh = JSON.parse(engine.snapshot());
  assert.equal(fresh.score, 0); assert.equal(fresh.lives, 3); assert.equal(fresh.remainingMs, 45000);
});
