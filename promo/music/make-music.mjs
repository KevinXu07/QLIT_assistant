// Synthesises the promo soundtrack (120 BPM, D major) straight to public/music.wav.
// Every hit is placed from src/timeline.json so cuts and UI moments land on the music.
import fs from 'node:fs';

const tl = JSON.parse(fs.readFileSync(new URL('../src/timeline.json', import.meta.url), 'utf8'));
const SR = 44100;
const SLOW = 1.2;
const DUR = tl.total / tl.fps;
const N = Math.ceil(DUR * SR);
const BEAT = 60 / tl.bpm;
const STEP = BEAT / 4;

const starts = [0, ...tl.cuts.map((c, i) => Math.round(c * tl.fps) - tl.trans[i] / 2)];
const at = (scene, localFrame) => (starts[scene] + localFrame * SLOW) / tl.fps;

const drL = new Float32Array(N), drR = new Float32Array(N);
const muL = new Float32Array(N), muR = new Float32Array(N);
const fxL = new Float32Array(N), fxR = new Float32Array(N);
const rev = new Float32Array(N);
const duck = new Float32Array(N).fill(1);

let seed = 1234567;
const rnd = () => {
  seed = (seed + 0x6d2b79f5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const noise = () => rnd() * 2 - 1;
const mtof = (m) => 440 * Math.pow(2, (m - 69) / 12);
const panG = (p) => [Math.cos(((p + 1) * Math.PI) / 4), Math.sin(((p + 1) * Math.PI) / 4)];
const onePole = (fc) => 1 - Math.exp((-2 * Math.PI * fc) / SR);

function put(bl, br, t, len, fn, {pan = 0, send = 0} = {}) {
  const i0 = Math.round(t * SR);
  const [gl, gr] = panG(pan);
  for (let n = 0; n < len * SR; n++) {
    const i = i0 + n;
    if (i < 0) continue;
    if (i >= N) break;
    const v = fn(n / SR, n);
    if (v === null) break;
    bl[i] += v * gl;
    br[i] += v * gr;
    if (send) rev[i] += v * send;
  }
}

/* ---------- drums ---------- */
function kick(t, amp = 1) {
  let ph = 0;
  put(drL, drR, t, 0.5, (x, n) => {
    const f = 44 + 120 * Math.exp(-x * 30);
    ph += (2 * Math.PI * f) / SR;
    const click = n < 60 ? noise() * 0.25 * (1 - n / 60) : 0;
    return (Math.tanh(Math.sin(ph) * 1.6) * Math.exp(-x * 6.5) + click) * amp;
  });
  const i0 = Math.round(t * SR);
  for (let n = 0; n < 0.32 * SR; n++) {
    const i = i0 + n;
    if (i >= N) break;
    const d = 1 - 0.6 * amp * Math.exp(-(n / SR) * 11);
    if (d < duck[i]) duck[i] = d;
  }
}
function clap(t, amp = 1, pan = 0) {
  let lo = 0, band = 0;
  const f = 2 * Math.sin((Math.PI * 1500) / SR);
  put(drL, drR, t, 0.4, (x) => {
    const burst = x < 0.03 ? Math.exp(-((x % 0.01) * 300)) : Math.exp(-(x - 0.03) * 16);
    const s = noise();
    lo += f * band;
    const hi = s - lo - 0.9 * band;
    band += f * hi;
    return band * burst * 0.9 * amp;
  }, {pan, send: 0.45});
}
function hat(t, amp = 0.2, open = false, pan = 0.25) {
  let lp = 0;
  const a = onePole(7000);
  put(drL, drR, t, open ? 0.3 : 0.07, (x) => {
    const s = noise();
    lp += a * (s - lp);
    return (s - lp) * Math.exp(-x * (open ? 13 : 70)) * amp;
  }, {pan, send: 0.05});
}
function crash(t, amp = 0.5) {
  let lp = 0;
  const a = onePole(4000);
  put(drL, drR, t, 2.4, (x) => {
    const s = noise();
    lp += a * (s - lp);
    return (s - lp) * Math.exp(-x * 2.2) * amp;
  }, {send: 0.4});
}

/* ---------- tonal ---------- */
function bass(t, m, dur, amp = 0.5) {
  const f = mtof(m);
  let ph = 0, lp = 0;
  const a = onePole(520);
  put(muL, muR, t, dur + 0.08, (x) => {
    ph += f / SR;
    const saw = 2 * (ph % 1) - 1;
    lp += a * (saw - lp);
    const env = Math.min(1, x / 0.004) * (x < dur ? 0.75 + 0.25 * Math.exp(-x * 9) : Math.exp(-(x - dur) * 50) * 0.75);
    return (lp * 0.8 + Math.sin(2 * Math.PI * ph) * 0.7) * env * amp;
  });
}
function pad(t, notes, dur, amp = 0.1, fc = 1400) {
  notes.forEach((m, k) => {
    [-0.07, 0, 0.07].forEach((det, v) => {
      const f = mtof(m + det);
      let ph = rnd(), l1 = 0, l2 = 0;
      const a = onePole(fc);
      const pan = (v - 1) * 0.6;
      put(muL, muR, t, dur + 0.8, (x) => {
        ph += f / SR;
        const saw = 2 * (ph % 1) - 1;
        l1 += a * (saw - l1);
        l2 += a * (l1 - l2);
        const env = Math.min(1, x / 0.35) * (x < dur ? 1 : Math.exp(-(x - dur) * 5));
        return l2 * env * amp * 0.33;
      }, {pan, send: 0.25});
    });
  });
}
function pluck(t, m, amp = 0.2, pan = 0, decay = 0.995, len = 1.4) {
  const p = Math.max(2, Math.round(SR / mtof(m)));
  const buf = new Float32Array(p);
  let lp = 0;
  for (let i = 0; i < p; i++) { lp += 0.5 * (noise() - lp); buf[i] = lp; }
  let idx = 0;
  put(muL, muR, t, len, () => {
    const nx = (idx + 1) % p;
    const v = buf[idx];
    buf[idx] = 0.5 * (buf[idx] + buf[nx]) * decay;
    idx = nx;
    return v * amp;
  }, {pan, send: 0.3});
}
function bell(t, m, amp = 0.15, pan = 0, bus = 'mu') {
  const f = mtof(m);
  const parts = [[1, 1, 3.2], [2.0, 0.35, 6], [3.0, 0.12, 9], [4.16, 0.08, 14]];
  const [bl, br] = bus === 'fx' ? [fxL, fxR] : [muL, muR];
  put(bl, br, t, 1.8, (x) => {
    let s = 0;
    for (const [r, g, d] of parts) s += Math.sin(2 * Math.PI * f * r * x) * g * Math.exp(-x * d);
    return s * Math.min(1, x / 0.003) * amp;
  }, {pan, send: 0.35});
}

/* ---------- effects ---------- */
function impact(t, amp = 1) {
  let ph = 0, lp = 0;
  const a = onePole(700);
  put(fxL, fxR, t, 2.6, (x) => {
    const f = 36 + 60 * Math.exp(-x * 8);
    ph += (2 * Math.PI * f) / SR;
    lp += a * (noise() - lp);
    return (Math.sin(ph) * Math.exp(-x * 1.8) * 0.9 + lp * Math.exp(-x * 7) * 1.4) * amp;
  }, {send: 0.5});
  kick(t, 0.8 * amp);
}
function sweepNoise(t0, dur, f0, f1, amp, panFrom = 0, panTo = 0, shape = (p) => p * p) {
  let lo = 0, band = 0;
  const i0 = Math.round(t0 * SR);
  for (let n = 0; n < dur * SR; n++) {
    const i = i0 + n;
    if (i < 0) continue;
    if (i >= N) break;
    const p = n / (dur * SR);
    const fc = f0 * Math.pow(f1 / f0, p);
    const f = 2 * Math.sin((Math.PI * Math.min(fc, 8000)) / SR);
    lo += f * band;
    const hi = noise() - lo - 0.6 * band;
    band += f * hi;
    const v = band * shape(p) * amp;
    const [gl, gr] = panG(panFrom + (panTo - panFrom) * p);
    fxL[i] += v * gl;
    fxR[i] += v * gr;
    rev[i] += v * 0.3;
  }
}
const riser = (t0, t1, amp = 0.35) => sweepNoise(t0, t1 - t0, 300, 7000, amp, -0.3, 0.3, (p) => p * p * p);
const whoosh = (tc, amp = 0.3, dir = 1) => sweepNoise(tc - 0.32, 0.46, 500, 5000, amp, -0.7 * dir, 0.7 * dir, (p) => Math.sin(Math.PI * Math.min(1, p * 1.15)) ** 2);
function pop(t, m = 84, amp = 0.12, pan = 0) {
  let ph = 0;
  put(fxL, fxR, t, 0.12, (x) => {
    ph += (2 * Math.PI * mtof(m) * (1 + 0.5 * Math.exp(-x * 60))) / SR;
    return Math.sin(ph) * Math.exp(-x * 38) * amp;
  }, {pan, send: 0.2});
}
function tick(t, amp = 0.18) {
  let lp = 0;
  const a = onePole(3500);
  put(fxL, fxR, t, 0.03, (x) => {
    lp += a * (noise() - lp);
    return lp * Math.exp(-x * 200) * amp;
  });
}
const lift = (t, amp = 0.14) => { sweepNoise(t - 0.05, 0.3, 900, 4500, amp * 1.4, 0, 0, (p) => Math.sin(Math.PI * p)); bell(t + 0.12, 93, amp * 0.5, 0, 'fx'); };

/* ---------- arrangement ---------- */
const CH = {
  D: {pad: [50, 54, 57, 61, 64], root: 38, arp: [62, 66, 69, 73, 76, 73, 69, 66]},
  Bm: {pad: [47, 50, 54, 57, 61], root: 35, arp: [59, 62, 66, 69, 73, 69, 66, 62]},
  G: {pad: [43, 47, 50, 54, 57], root: 31, arp: [55, 59, 62, 66, 69, 66, 62, 59]},
  A: {pad: [45, 49, 52, 54, 59], root: 33, arp: [57, 61, 64, 66, 71, 66, 64, 61]},
};
const stab = (t, ch, amp = 0.14) => CH[ch].pad.slice(1).forEach((m, k) => pluck(t + k * 0.006, m + 12, amp, (k - 2) * 0.25, 0.993, 1.2));

// Intro: stamp impact, warm pad, rising into the first word.
impact(at(0, 12.7), 0.6);
pad(0.2, CH.D.pad, 2.7, 0.07, 700);
[[0.62, 69], [0.86, 74], [1.1, 78]].forEach(([t, m], k) => bell(t, m, 0.09, (k - 1) * 0.4));
riser(1.5, 3.0, 0.22);
whoosh(3.0, 0.22);

// Word cuts: one kick, one chord stab and one rising bell note per word.
const words = ['D', 'Bm', 'G', 'A', 'A'];
const melody = [74, 76, 78, 81, 83];
words.forEach((ch, k) => {
  const t = 3 + k * BEAT;
  kick(t, 0.85);
  if (k % 2) clap(t, 0.5);
  stab(t, ch, 0.11);
  bell(t, melody[k] + 12, 0.1, (k - 2) * 0.3);
  hat(t + BEAT / 2, 0.12);
});
pad(3.0, CH.D.pad, 0.5, 0.06, 900);
pad(3.5, CH.Bm.pad, 0.5, 0.06, 900);
pad(4.0, CH.G.pad, 0.5, 0.06, 900);
pad(4.5, CH.A.pad, 1.0, 0.06, 900);
// "全都放进一页": full hit, then a build into the groove.
kick(5.5, 1);
crash(5.5, 0.35);
stab(5.5, 'D', 0.16);
bell(5.5, 86, 0.12);
pad(5.5, CH.D.pad, 1.4, 0.08, 1100);
bass(5.5, 38, 0.9, 0.45);
for (let s = 0; s < 12; s++) hat(5.5 + s * STEP, 0.05 + s * 0.012, false, s % 2 ? 0.3 : -0.3);
for (let s = 0; s < 8; s++) clap(6.0 + s * (STEP * 1), 0.12 + s * 0.06, s % 2 ? 0.2 : -0.2);
riser(5.8, 7.0, 0.25);
whoosh(7.0, 0.3);

// Main groove 7s–24s, four-bar loop D–Bm–G–A.
const MEL = [
  [[0, 78, 3], [3, 81, 3], [6, 83, 2], [8, 81, 2], [10, 78, 2], [12, 76, 4]],
  [[0, 74, 3], [3, 76, 3], [6, 78, 6], [12, 81, 2], [14, 83, 2]],
  [[0, 86, 3], [3, 83, 3], [6, 81, 2], [8, 78, 4], [12, 76, 2], [14, 78, 2]],
  [[0, 76, 6], [6, 74, 2], [8, 76, 4], [12, 69, 4]],
];
const ORDER = ['D', 'Bm', 'G', 'A'];
function groove(t0, bars, {arp = true, melody = false, drums = true, startBar = 0} = {}) {
  for (let b = 0; b < bars; b++) {
    const t = t0 + b * 4 * BEAT;
    const bi = (startBar + b) % 4;
    const ch = CH[ORDER[bi]];
    const len = Math.min(4 * BEAT, DUR - t);
    pad(t, ch.pad, len - 0.05, 0.075, 1500);
    if (drums) {
      [0, 6, 8].forEach((s) => kick(t + s * STEP, s === 6 ? 0.7 : 0.95));
      [4, 12].forEach((s) => clap(t + s * STEP, 0.55));
      for (let s = 0; s < 16; s++) hat(t + s * STEP + (s % 2 ? 0.012 : 0), s % 4 === 2 ? 0.16 : 0.06, s % 4 === 2, s % 2 ? 0.3 : -0.2);
      if (bi === 3) { kick(t + 14 * STEP, 0.6); clap(t + 15 * STEP, 0.3); }
    }
    [[0, 0, 0.32], [3, 0, 0.1], [6, 0, 0.2], [8, 0, 0.32], [11, 12, 0.1], [14, 7, 0.12]].forEach(([s, o, d]) => bass(t + s * STEP, ch.root + o, d, 0.42));
    if (arp) for (let s = 0; s < 8; s++) pluck(t + s * 2 * STEP, ch.arp[s], 0.07, s % 2 ? 0.45 : -0.45, 0.994, 0.8);
    if (melody) MEL[bi].forEach(([s, m, l]) => bell(t + s * STEP, m, 0.1 * Math.min(1, l / 3 + 0.4), 0.1));
  }
}
groove(7, 2, {arp: false});
groove(11, 2, {startBar: 2});
groove(15, 4, {melody: true});
groove(23, 0.5);
crash(7, 0.25);
crash(15, 0.2);
tl.cuts.slice(2, 6).forEach((c, k) => whoosh(c, 0.18, k % 2 ? -1 : 1));

// Night breakdown under the dark-mode scene, building back into the outro.
whoosh(24, 0.25, -1);
pad(24, CH.Bm.pad, 2, 0.08, 800);
pad(26, CH.G.pad, 1, 0.08, 900);
pad(27, CH.A.pad, 1, 0.08, 1200);
for (let s = 0; s < 8; s++) pluck(24 + s * 0.5, CH[s < 4 ? 'Bm' : s < 6 ? 'G' : 'A'].arp[s % 8] + 12, 0.06, s % 2 ? 0.5 : -0.5, 0.996, 1.2);
bass(24, 35, 1.9, 0.3);
bass(26, 31, 0.9, 0.3);
bass(27, 33, 0.9, 0.3);
for (let s = 0; s < 4; s++) kick(27 + s * BEAT, 0.5 + s * 0.12);
for (let s = 0; s < 8; s++) clap(27 + s * STEP * 2, 0.08 + s * 0.05, 0);
riser(26.6, 28, 0.25);

// Outro: resolve on D with the second stamp, then let it ring out.
impact(at(8, 8.7), 0.6);
crash(28, 0.35);
groove(28, 1);
stab(28, 'D', 0.13);
pad(30, CH.G.pad, 1, 0.07, 1400);
pad(31, CH.A.pad, 1, 0.07, 1400);
bass(30, 31, 0.9, 0.38);
bass(31, 33, 0.9, 0.38);
[0, 2, 4, 6].forEach((s) => kick(30 + s * STEP * 2, 0.75));
[2, 6].forEach((s) => clap(30 + s * STEP * 2, 0.45));
kick(32, 1);
crash(32, 0.3);
stab(32, 'D', 0.15);
pad(32, CH.D.pad, 1.0, 0.08, 1500);
bass(32, 38, 0.95, 0.4);
bell(32, 86, 0.12);
bell(32.02, 78, 0.08);

// UI moments.
lift(at(2, 66)); pop(at(2, 100), 86); pop(at(2, 108), 88);
lift(at(3, 58)); lift(at(3, 68)); pop(at(3, 88), 86);
for (let k = 0; k < 6; k++) pop(at(4, 30 + k * 7), [81, 83, 86, 88, 90, 93][k], 0.1, k % 2 ? 0.4 : -0.4);
lift(at(5, 46)); lift(at(5, 58)); tick(at(5, 88)); bell(at(5, 98), 86, 0.08, 0, 'fx'); bell(at(5, 98) + 0.09, 93, 0.08, 0, 'fx');
sweepNoise(at(6, 46), 0.3, 1200, 4000, 0.12, -0.6, 0.6, (p) => Math.sin(Math.PI * p));
lift(at(6, 84)); bell(at(6, 112), 88, 0.1, 0, 'fx'); bell(at(6, 112) + 0.28, 84, 0.1, 0, 'fx');
tick(at(7, 28), 0.25); sweepNoise(at(7, 36), 0.9, 6000, 300, 0.2, 0.6, -0.4, (p) => Math.sin(Math.PI * p)); lift(at(7, 92), 0.1);
pop(at(8, 62), 81, 0.12); bell(at(8, 96), 98, 0.05, 0.3, 'fx');

/* ---------- reverb + master ---------- */
function reverb(input, offset) {
  const combs = [1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116].map((l) => ({b: new Float32Array(l + offset), i: 0, s: 0}));
  const aps = [556, 441, 341, 225].map((l) => ({b: new Float32Array(l + offset), i: 0}));
  const out = new Float32Array(N);
  for (let n = 0; n < N; n++) {
    const x = input[n] * 0.015;
    let acc = 0;
    for (const c of combs) {
      const y = c.b[c.i];
      c.s = y * 0.75 + c.s * 0.25;
      c.b[c.i] = x + c.s * 0.86;
      c.i = (c.i + 1) % c.b.length;
      acc += y;
    }
    for (const a of aps) {
      const y = a.b[a.i];
      a.b[a.i] = acc + y * 0.5;
      a.i = (a.i + 1) % a.b.length;
      acc = y - acc;
    }
    out[n] = acc;
  }
  return out;
}
const rvL = reverb(rev, 0);
const rvR = reverb(rev, 23);
const outL = new Float32Array(N), outR = new Float32Array(N);
let energy = 0;
for (let n = 0; n < N; n++) {
  outL[n] = drL[n] * 0.8 + muL[n] * duck[n] + fxL[n] * 0.9 + rvL[n] * 0.9;
  outR[n] = drR[n] * 0.8 + muR[n] * duck[n] + fxR[n] * 0.9 + rvR[n] * 0.9;
  if (n > 7 * SR && n < 24 * SR) energy += outL[n] ** 2 + outR[n] ** 2;
}
// Level the groove to about -14 dBFS RMS; tanh then acts as a soft limiter for the impacts.
const drive = Math.pow(10, -14 / 20) / Math.sqrt(energy / (2 * 17 * SR));
let peak = 0;
for (let n = 0; n < N; n++) {
  const fade = Math.min(1, n / (0.01 * SR), (N - n) / (0.9 * SR));
  outL[n] = Math.tanh(outL[n] * drive) * fade;
  outR[n] = Math.tanh(outR[n] * drive) * fade;
  peak = Math.max(peak, Math.abs(outL[n]), Math.abs(outR[n]));
}
const gain = 0.95 / peak;
const wav = Buffer.alloc(44 + N * 4);
wav.write('RIFF', 0); wav.writeUInt32LE(36 + N * 4, 4); wav.write('WAVE', 8);
wav.write('fmt ', 12); wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20); wav.writeUInt16LE(2, 22);
wav.writeUInt32LE(SR, 24); wav.writeUInt32LE(SR * 4, 28); wav.writeUInt16LE(4, 32); wav.writeUInt16LE(16, 34);
wav.write('data', 36); wav.writeUInt32LE(N * 4, 40);
for (let n = 0; n < N; n++) {
  wav.writeInt16LE(Math.round(Math.max(-1, Math.min(1, outL[n] * gain)) * 32767), 44 + n * 4);
  wav.writeInt16LE(Math.round(Math.max(-1, Math.min(1, outR[n] * gain)) * 32767), 46 + n * 4);
}
fs.writeFileSync(new URL('../public/music.wav', import.meta.url), wav);
console.log(`music.wav ${DUR.toFixed(2)}s peak ${peak.toFixed(3)} gain ${gain.toFixed(2)}`);
