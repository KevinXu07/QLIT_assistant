// 匿名使用统计参考实现（零依赖，Node.js 自带模块即可运行）
//
// 用途：配合 App 内 UsageStatsReporter，提供两个端点：
//   GET /ping?uid=<uuid>&brand=&model=&os=&sdk=&v=&_=<millis>
//       记录一条匿名设备 ping（JSONL 追加写入 data/pings.jsonl）
//   GET /stats?token=<STATS_TOKEN>
//       聚合输出：设备总数 / 品牌分布 / 系统分布 / App 版本分布 / 每日活跃
//
// 启动：node analytics-example.js
// 可选环境变量：
//   PORT=8080                监听端口（默认 8080）
//   STATS_TOKEN=change-me    /stats 的访问口令（默认 change-me，务必修改）
//   DATA_DIR=./data          数据目录（默认 ./data）
//
// 接入现有后端：也可以只参考 /ping 的写入格式，把记录改写成
// 你服务端惯用的存储（数据库 / 日志 / KV），字段保持一致即可。

'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = parseInt(process.env.PORT || '8080', 10);
const STATS_TOKEN = process.env.STATS_TOKEN || 'change-me';
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, 'data');
const PINGS_FILE = path.join(DATA_DIR, 'pings.jsonl');

// 单字段长度上限，防止恶意超长写入
const MAX_FIELD_LEN = 64;

function sanitize(value) {
  return String(value == null ? '' : value).trim().slice(0, MAX_FIELD_LEN);
}

function appendPing(record) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.appendFileSync(PINGS_FILE, JSON.stringify(record) + '\n');
}

function readPings() {
  if (!fs.existsSync(PINGS_FILE)) return [];
  return fs.readFileSync(PINGS_FILE, 'utf8')
    .split('\n')
    .filter(Boolean)
    .map((line) => {
      try { return JSON.parse(line); } catch (_) { return null; }
    })
    .filter(Boolean);
}

function tally(records, keyFn) {
  const counts = {};
  for (const record of records) {
    const key = keyFn(record) || 'unknown';
    counts[key] = (counts[key] || 0) + 1;
  }
  return Object.fromEntries(
    Object.entries(counts).sort((a, b) => b[1] - a[1])
  );
}

function buildStats() {
  const pings = readPings();
  const devices = new Set(pings.map((p) => p.uid));

  const byDay = {};
  const lastSeen = {};
  for (const p of pings) {
    byDay[p.day] = (byDay[p.day] || 0) + 1;
    if (!lastSeen[p.uid] || p.day > lastSeen[p.uid]) lastSeen[p.uid] = p.day;
  }

  return {
    generatedAt: new Date().toISOString(),
    totalPings: pings.length,
    totalDevices: devices.size,
    // 按设备去重的品牌分布（一台设备只计一次）
    devicesByBrand: tally(
    Object.entries(lastSeen).map(([uid, day]) => pings.find((p) => p.uid === uid && p.day === day)),
      (p) => p && p.brand
    ),
    // 全部 ping 的系统版本分布（反映每日活跃的系统构成）
    pingsByOs: tally(pings, (p) => `Android ${p.os} (API ${p.sdk})`),
    pingsByAppVersion: tally(pings, (p) => p.v),
    pingsByModel: tally(pings, (p) => `${p.brand} ${p.model}`),
    dailyActivePings: byDay,
  };
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  res.setHeader('Cache-Control', 'no-store');

  if (url.pathname === '/ping') {
    const q = url.searchParams;
    if (!q.get('uid')) {
      res.writeHead(400).end();
      return;
    }
    appendPing({
      day: new Date().toISOString().slice(0, 10),
      ts: Date.now(),
      uid: sanitize(q.get('uid')),
      brand: sanitize(q.get('brand')),
      model: sanitize(q.get('model')),
      os: sanitize(q.get('os')),
      sdk: parseInt(sanitize(q.get('sdk')) || '0', 10) || 0,
      v: sanitize(q.get('v')),
    });
    res.writeHead(204).end();
    return;
  }

  if (url.pathname === '/stats') {
    if (url.searchParams.get('token') !== STATS_TOKEN) {
      res.writeHead(403).end();
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(buildStats(), null, 2));
    return;
  }

  res.writeHead(404).end();
});

server.listen(PORT, () => {
  console.log(`analytics listening on :${PORT} (data: ${DATA_DIR})`);
});
