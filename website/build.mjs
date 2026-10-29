// 生成可直接上传 EdgeOne Pages 的静态包：node website/build.mjs
// 产物：build/website/（目录）和 build/qlit-website.zip（index.html 在 zip 根目录）
// assets/ 下的文件名会带上内容哈希，配合 edgeone.json 里的一年强缓存；HTML 每次都回源校验。
import { createHash } from 'node:crypto';
import { cpSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { basename, dirname, extname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateRawSync } from 'node:zlib';

const SRC = dirname(fileURLToPath(import.meta.url));
const BUILD = join(SRC, '..', 'build');
const OUT = join(BUILD, 'website');
const ZIP = join(BUILD, 'qlit-website.zip');
const ENTRIES = ['index.html', '404.html', 'edgeone.json', 'assets'];
const PAGES = ['index.html', '404.html'];

const walk = (d) => readdirSync(d, { withFileTypes: true })
  .flatMap((e) => (e.isDirectory() ? walk(join(d, e.name)) : [join(d, e.name)]));

rmSync(OUT, { recursive: true, force: true });
mkdirSync(OUT, { recursive: true });
for (const e of ENTRIES) cpSync(join(SRC, e), join(OUT, e), { recursive: true });

// 许可证保持原名，方便直接打开
const assets = walk(join(OUT, 'assets')).filter((f) => extname(f) !== '.txt');
const seen = new Set();
for (const f of assets) {
  if (seen.has(basename(f))) throw new Error(`assets 下文件名重复：${basename(f)}`);
  seen.add(basename(f));
}

// 自己写的 css/js 可能引用字体和图片，放到最后，哈希才能覆盖改写后的内容
const rewritable = (f) => /\.(css|js)$/.test(f) && !f.endsWith('.min.js');
const renamed = new Map();
const rewrite = (s) => {
  for (const [from, to] of renamed) s = s.split('/' + from).join('/' + to);
  return s;
};
for (const f of [...assets.filter((f) => !rewritable(f)), ...assets.filter(rewritable)]) {
  if (rewritable(f)) writeFileSync(f, rewrite(readFileSync(f, 'utf8')));
  const hash = createHash('sha256').update(readFileSync(f)).digest('hex').slice(0, 8);
  const ext = extname(f);
  const name = `${basename(f, ext)}.${hash}${ext}`;
  renameSync(f, join(dirname(f), name));
  renamed.set(basename(f), name);
}

for (const p of PAGES) {
  const file = join(OUT, p);
  const html = rewrite(readFileSync(file, 'utf8'));
  for (const from of renamed.keys()) {
    if (html.includes('/' + from)) throw new Error(`${p} 里还有未替换的引用：${from}`);
  }
  writeFileSync(file, html);
}

// ---- 最小 zip 写入（deflate，UTF-8 文件名），免得依赖系统压缩工具 ----
const CRC = new Uint32Array(256).map((_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
const crc32 = (buf) => {
  let c = 0xffffffff;
  for (const b of buf) c = CRC[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
};
function writeZip(dir, out) {
  const now = new Date();
  const time = (now.getHours() << 11) | (now.getMinutes() << 5) | (now.getSeconds() >> 1);
  const date = ((now.getFullYear() - 1980) << 9) | ((now.getMonth() + 1) << 5) | now.getDate();
  const local = [];
  const central = [];
  let offset = 0;
  const files = walk(dir).sort();
  for (const f of files) {
    const name = Buffer.from(relative(dir, f).split('\\').join('/'));
    const raw = readFileSync(f);
    const data = deflateRawSync(raw, { level: 9 });
    const crc = crc32(raw);
    const h = Buffer.alloc(30);
    h.writeUInt32LE(0x04034b50, 0); h.writeUInt16LE(20, 4); h.writeUInt16LE(0x0800, 6); h.writeUInt16LE(8, 8);
    h.writeUInt16LE(time, 10); h.writeUInt16LE(date, 12); h.writeUInt32LE(crc, 14);
    h.writeUInt32LE(data.length, 18); h.writeUInt32LE(raw.length, 22); h.writeUInt16LE(name.length, 26);
    const c = Buffer.alloc(46);
    c.writeUInt32LE(0x02014b50, 0); c.writeUInt16LE(20, 4); c.writeUInt16LE(20, 6); c.writeUInt16LE(0x0800, 8);
    c.writeUInt16LE(8, 10); c.writeUInt16LE(time, 12); c.writeUInt16LE(date, 14); c.writeUInt32LE(crc, 16);
    c.writeUInt32LE(data.length, 20); c.writeUInt32LE(raw.length, 24); c.writeUInt16LE(name.length, 28);
    c.writeUInt32LE(offset, 42);
    local.push(h, name, data);
    central.push(c, name);
    offset += h.length + name.length + data.length;
  }
  const cd = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0); end.writeUInt16LE(files.length, 8); end.writeUInt16LE(files.length, 10);
  end.writeUInt32LE(cd.length, 12); end.writeUInt32LE(offset, 16);
  writeFileSync(out, Buffer.concat([...local, cd, end]));
  return files.length;
}

rmSync(ZIP, { force: true });
const count = writeZip(OUT, ZIP);
console.log(`${count} files -> ${relative(join(SRC, '..'), ZIP)}`);
for (const [from, to] of renamed) console.log(`  ${from} -> ${to}`);
