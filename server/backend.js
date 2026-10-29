// QLIT 小助手自建后端（零依赖 / Node.js）
//
// 三件事：
//   1. 应用更新：GET /update.json（老版本 App 兼容契约）、GET /apk/<file>、GET /latest.apk
//   2. 匿名统计：GET /ping（设备 ping + 同意/拒绝结果）
//   3. 管理网页：GET / 打开交互式后台（上传安装包、发布更新日志、看统计）
// 管理页模板位于同目录的 admin-page.js。
//
// 启动：node backend.js
// 环境变量：
//   PORT=8080                 监听端口（默认 8080，建议 nginx 反代到 443）
//   ADMIN_TOKEN=change-me     管理口令（务必修改）
//   DATA_DIR=./data           数据目录（默认 ./data）
//   PUBLIC_BASE=https://api.kevinxu.cn   对外基址，用于拼 apkUrl
//
// 建议用 pm2 常驻：pm2 start backend.js --name qlit-backend

'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { adminPageHtml } = require('./admin-page');

const PORT = parseInt(process.env.PORT || '8080', 10);
let ADMIN_TOKEN = process.env.ADMIN_TOKEN || '';
if (!ADMIN_TOKEN || ADMIN_TOKEN === 'change-me') {
  ADMIN_TOKEN = crypto.randomBytes(16).toString('hex');
  console.warn('[qlit-backend] ADMIN_TOKEN 未配置或为默认值，已生成随机口令（仅本次启动有效，请设置环境变量后重启）：' + ADMIN_TOKEN);
}
const DATA_DIR = path.resolve(process.env.DATA_DIR || path.join(__dirname, 'data'));
const PUBLIC_BASE = (process.env.PUBLIC_BASE || 'https://api.kevinxu.cn').replace(/\/+$/, '');
const APK_DIR = path.join(DATA_DIR, 'apks');
const PINGS_FILE = path.join(DATA_DIR, 'pings.jsonl');
const STATE_FILE = path.join(DATA_DIR, 'state.json');

const MAX_FIELD_LEN = 64;
const MAX_APK_BYTES = 300 * 1024 * 1024; // APK 上传上限 300MB
const pingHits = {}; // ip -> [timestamps] 简易限速

// ── 工具 ──────────────────────────────────────────────────────

function ensureDirs() {
  fs.mkdirSync(APK_DIR, { recursive: true });
}

function sanitize(value) {
  return String(value == null ? '' : value).trim().slice(0, MAX_FIELD_LEN);
}

function safeFileName(name) {
  const base = path.basename(String(name || ''));
  return /^[\w.\-]+$/.test(base) && base.length <= 120 ? base : null;
}

function readJson(file, fallback) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (_) {
    return fallback;
  }
}

function writeJson(file, data) {
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
  fs.renameSync(tmp, file);
}

function appendPing(record) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  try {
    if (fs.existsSync(PINGS_FILE) && fs.statSync(PINGS_FILE).size > 5 * 1024 * 1024) {
      fs.renameSync(PINGS_FILE, PINGS_FILE + '.' + Date.now() + '.archive');
      // 归档最多保留 10 份
      const archives = fs.readdirSync(DATA_DIR)
        .filter((f) => f.startsWith('pings.jsonl.') && f.endsWith('.archive'))
        .sort();
      while (archives.length > 10) {
        const oldest = archives.shift();
        try { fs.unlinkSync(path.join(DATA_DIR, oldest)); } catch (_) {}
      }
    }
  } catch (_) {}
  fs.appendFileSync(PINGS_FILE, JSON.stringify(record) + '\n');
}

function readPings() {
  // 归档 + 当前文件一起读，避免轮转后统计被重置
  let files = [];
  try {
    files = fs.readdirSync(DATA_DIR)
      .filter((f) => f.startsWith('pings.jsonl'))
      .sort()
      // 当前文件排最后读取：同键冲突时以最新记录为准
      .sort((a, b) => (a === 'pings.jsonl') - (b === 'pings.jsonl'))
      .map((f) => path.join(DATA_DIR, f));
  } catch (_) {}
  const out = [];
  for (const f of files) {
    try {
      for (const line of fs.readFileSync(f, 'utf8').split('\n')) {
        if (!line) continue;
        try {
          const r = JSON.parse(line);
          if (r) out.push(r);
        } catch (_) {}
      }
    } catch (_) {}
  }
  return out;
}

function tally(records, keyFn) {
  const counts = {};
  for (const r of records) {
    const key = keyFn(r) || 'unknown';
    counts[key] = (counts[key] || 0) + 1;
  }
  return Object.entries(counts).sort((a, b) => b[1] - a[1]);
}

function json(res, code, data) {
  const body = JSON.stringify(data, null, 2);
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
  });
  res.end(body);
}

// ── 统计聚合 ──────────────────────────────────────────────────

function buildStats() {
  const pings = readPings();
  const devices = new Map(); // uid -> { lastDay, brand, model, os, sdk, consent }
  const byDay = {};
  for (const p of pings) {
    const d = devices.get(p.uid) || {};
    if (!d.firstDay) d.firstDay = p.day;
    if (!d.lastDay || p.day > d.lastDay) {
      d.lastDay = p.day;
      if (p.brand) d.brand = p.brand;
      if (p.model) d.model = p.model;
      if (p.os) d.os = p.os;
      if (p.sdk) d.sdk = p.sdk;
      if (p.rom) d.rom = p.rom;
    }
    if (p.consent !== undefined) d.consent = p.consent === '1' || p.consent === 1;
    devices.set(p.uid, d);
    byDay[p.day] = (byDay[p.day] || 0) + 1;
  }

  const deviceList = [...devices.entries()].map(([uid, d]) => ({ uid, ...d }));
  const agreed = deviceList.filter((d) => d.consent === true).length;
  const rejected = deviceList.filter((d) => d.consent === false).length;
  const undecided = deviceList.length - agreed - rejected;

  return {
    generatedAt: new Date().toISOString(),
    totalPings: pings.length,
    totalDevices: devices.size,
    consent: { agreed, rejected, undecided },
    devicesByBrand: tally(deviceList.filter((d) => d.brand), (d) => d.brand),
    devicesByOs: tally(deviceList.filter((d) => d.os), (d) => `Android ${d.os} (API ${d.sdk || '?'})`),
    devicesByRom: tally(deviceList.filter((d) => d.rom), (d) => d.rom),
    devicesByModel: tally(deviceList.filter((d) => d.model), (d) => `${d.brand || ''} ${d.model}`.trim()),
    pingsByAppVersion: tally(pings.filter((p) => p.v), (p) => p.v),
    dailyPings: Object.fromEntries(Object.entries(byDay).sort()),
  };
}

// ── 请求处理 ──────────────────────────────────────────────────

function readBody(req, callback) {
  // 大文件（apk）直接流式落盘由调用方处理，这里只给 JSON 用的小 body
  let size = 0;
  const chunks = [];
  req.on('data', (c) => {
    size += c.length;
    if (size > 1 << 20) { req.destroy(); return; }
    chunks.push(c);
  });
  req.on('end', () => callback(Buffer.concat(chunks)));
}

const server = http.createServer((req, res) => {
  // 顶层兜底：畸形 URI/Range 等未预期异常返回 400，绝不带崩进程
  try {
    handleRequest(req, res);
  } catch (e) {
    try { res.writeHead(400); res.end(); } catch (_) {}
  }
});

function handleRequest(req, res) {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const p = url.pathname;
  res.setHeader('Cache-Control', 'no-store');

  // —— 匿名统计 ping（无口令；字段见 UsageStatsReporter）——
  if (p === '/ping' && req.method === 'GET') {
    const q = url.searchParams;
    if (!sanitize(q.get('uid'))) { res.writeHead(400).end(); return; }
    const ip = (req.headers['x-forwarded-for'] || '').split(',')[0].trim()
      || req.socket.remoteAddress || 'unknown';
    const nowMs = Date.now();
    for (const k of Object.keys(pingHits)) {
      pingHits[k] = pingHits[k].filter((t) => nowMs - t < 60000);
      if (pingHits[k].length === 0) delete pingHits[k];
    }
    pingHits[ip] = (pingHits[ip] || []);
    pingHits[ip].push(nowMs);
    if (pingHits[ip].length > 10) { res.writeHead(429).end(); return; }
    const consent = q.get('consent');
    try {
    appendPing({
      day: new Date().toISOString().slice(0, 10),
      ts: Date.now(),
      uid: sanitize(q.get('uid')),
      brand: sanitize(q.get('brand')),
      model: sanitize(q.get('model')),
      os: sanitize(q.get('os')),
      sdk: parseInt(sanitize(q.get('sdk')) || '0', 10) || 0,
      rom: sanitize(q.get('rom')),
      v: sanitize(q.get('v')),
      ...(consent !== null ? { consent: sanitize(consent) } : {}),
      });
    } catch (_) {
      // 写盘失败返回 500：客户端保留补发标记，下次启动重试
      res.writeHead(500).end();
      return;
    }
    res.writeHead(204).end();
    return;
  }

  // —— 应用更新检查（老契约：versionCode/versionName/apkUrl/changelog）——
  // 允许跨域读取：官网用它显示最新版本号和下载地址（内容本就公开）
  if (p === '/update.json' && req.method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET', 'Access-Control-Max-Age': '86400' }).end();
    return;
  }
  if (p === '/update.json' && req.method === 'GET') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    const state = readJson(STATE_FILE, null);
    if (!state || !state.versionCode) {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' }).end('{}');
      return;
    }
    json(res, 200, {
      versionCode: state.versionCode,
      versionName: state.versionName,
      apkUrl: state.apkUrl || (PUBLIC_BASE + '/apk/' + state.apkFile),
      changelog: state.changelog || '',
    });
    return;
  }

  // —— 固定下载地址：始终跳到当前线上版本的安装包（官网按钮、二维码用）——
  if (p === '/latest.apk' && (req.method === 'GET' || req.method === 'HEAD')) {
    const state = readJson(STATE_FILE, null);
    if (!state || !state.apkFile) { res.writeHead(404).end(); return; }
    const target = state.apkUrl || (PUBLIC_BASE + '/apk/' + encodeURIComponent(state.apkFile));
    res.writeHead(302, { Location: target, 'Access-Control-Allow-Origin': '*' }).end();
    return;
  }

  // —— APK 下载（支持断点续传）——
  if (p.startsWith('/apk/') && req.method === 'GET') {
    let name = null;
    try { name = safeFileName(decodeURIComponent(p.slice(5))); } catch (_) { name = null; }
    const file = name && path.join(APK_DIR, name);
    if (!file || !fs.existsSync(file)) { res.writeHead(404).end(); return; }
    let size = 0;
    try { size = fs.statSync(file).size; } catch (_) { res.writeHead(404).end(); return; }
    const range = req.headers.range;
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Accept-Ranges', 'bytes');
    if (range) {
      const m = /bytes=(\d*)-(\d*)/.exec(range);
      const start = m && m[1] ? parseInt(m[1], 10) : 0;
      const end = m && m[2] ? parseInt(m[2], 10) : size - 1;
      if (isNaN(start) || isNaN(end) || start < 0 || start > end || end >= size) {
        res.writeHead(416, { 'Content-Range': `bytes */${size}` });
        res.end();
        return;
      }
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${size}`,
        'Content-Length': end - start + 1,
      });
      const rs = fs.createReadStream(file, { start, end });
      rs.on('error', () => { try { res.destroy(); } catch (_) {} });
      rs.pipe(res);
    } else {
      res.writeHead(200, { 'Content-Length': size });
      const rs = fs.createReadStream(file);
      rs.on('error', () => { try { res.destroy(); } catch (_) {} });
      rs.pipe(res);
    }
    return;
  }

  // —— 管理页（页面本身不含数据，接口都校验口令）——
  if ((p === '/' || p === '/admin') && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(adminPageHtml());
    return;
  }

  // —— 以下全部需要口令（仅 header，不再接受查询串以免泄露进日志）——
  const token = req.headers['x-admin-token'];
  if (token !== ADMIN_TOKEN) { res.writeHead(403).end(); return; }

  if (p === '/admin/state' && req.method === 'GET') {
    const state = readJson(STATE_FILE, {});
    let apks = [];
    try { apks = fs.readdirSync(APK_DIR).filter((f) => f.endsWith('.apk')); } catch (_) {}
    json(res, 200, { update: state.versionCode ? state : null, apks });
    return;
  }

  if (p === '/admin/apk' && req.method === 'PUT') {
    const name = safeFileName(url.searchParams.get('name'));
    if (!name || !name.toLowerCase().endsWith('.apk')) { res.writeHead(400).end(); return; }
    const finalFile = path.join(APK_DIR, name);
    const tmpFile = finalFile + '.' + Date.now() + '.' + process.pid + '.uploading';
    let received = 0;
    let done = false;
    const cleanup = () => { try { fs.unlinkSync(tmpFile); } catch (_) {} };
    req.on('error', () => {}); // 上传中途客户端断开等 IO 错误：静默，防未处理 error 事件带崩进程
    req.on('data', (c) => {
      received += c.length;
      if (received > MAX_APK_BYTES && !done) {
        done = true;
        // 先结束响应（413 可达客户端），再断开上传流并清理临时文件
        res.writeHead(413, { Connection: 'close' });
        res.end();
        stream.destroy();
        cleanup();
      }
    });
    const stream = fs.createWriteStream(tmpFile);
    req.pipe(stream);
    stream.on('finish', () => {
      if (done) { cleanup(); return; }
      try { fs.renameSync(tmpFile, finalFile); res.writeHead(204).end(); }
      catch (_) { cleanup(); res.writeHead(500).end(); }
    });
    stream.on('error', () => { cleanup(); if (!done) res.writeHead(500).end(); });
    req.on('aborted', () => { cleanup(); });
    return;
  }

  if (p === '/admin/publish' && req.method === 'POST') {
    readBody(req, (body) => {
      try {
        const data = JSON.parse(body.toString('utf8') || '{}');
        const versionCode = parseInt(data.versionCode, 10);
        const versionName = String(data.versionName || '').trim().slice(0, 32);
        const apkFile = safeFileName(data.apkFile);
        if (!versionCode || !versionName || !apkFile) { json(res, 400, { error: '缺少字段' }); return; }
        if (!fs.existsSync(path.join(APK_DIR, apkFile))) { json(res, 400, { error: '安装包不存在' }); return; }
        const prev = readJson(STATE_FILE, {});
        if (prev.versionCode && versionCode <= Number(prev.versionCode)) {
          json(res, 400, { error: `版本号必须大于当前版本 ${prev.versionCode}` });
          return;
        }
        const state = {
          versionCode,
          versionName,
          apkFile,
          apkUrl: PUBLIC_BASE + '/apk/' + apkFile,
          changelog: String(data.changelog || '').slice(0, 2000),
          publishedAt: new Date().toISOString(),
          publishedFrom: prev.publishedAt || null,
        };
        writeJson(STATE_FILE, state);
        json(res, 200, { ok: true, state });
      } catch (e) {
        json(res, 400, { error: '参数错误' });
      }
    });
    return;
  }

  if (p === '/stats.json' && req.method === 'GET') {
    json(res, 200, buildStats());
    return;
  }

  if (p === '/admin/pings.csv' && req.method === 'GET') {
    res.writeHead(200, {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': 'attachment; filename="pings.csv"',
    });
    res.end('day,ts,uid,brand,model,os,sdk,rom,v,consent\n' +
      readPings().map((r) =>
        [r.day, r.ts, r.uid, r.brand, r.model, r.os, r.sdk, r.rom || '', r.v, r.consent == null ? '' : r.consent]
          .map((cell) => {
            let s = String(cell == null ? '' : cell);
            if (/[",\r\n]/.test(s)) s = '"' + s.replace(/"/g, '""') + '"';
            if (/^[=+\-@]/.test(s)) s = "'" + s;
            return s;
          }).join(',')
      ).join('\n'));
    return;
  }

  res.writeHead(404).end();
}

ensureDirs();
server.listen(PORT, () => {
  console.log(`qlit-backend listening on :${PORT} (data: ${DATA_DIR})`);
});
