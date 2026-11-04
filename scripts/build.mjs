import { cp, mkdir, rm } from 'node:fs/promises';
await rm('dist', { recursive: true, force: true });
await mkdir('dist', { recursive: true });
for (const item of ['index.html','manifest.webmanifest','icon.svg','sw.js','src']) {
  await cp(item, `dist/${item}`, { recursive: true });
}
console.log('Built offline web app into dist/');
