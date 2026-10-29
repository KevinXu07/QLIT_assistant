/* ---------------- data ---------------- */
const SLOTS = [
  { k: 1, a: 1, b: 2, start: '08:30', end: '10:05' },
  { k: 2, a: 3, b: 4, start: '10:20', end: '11:55' },
  { k: 3, a: 5, b: 6, start: '12:30', end: '14:05' },
  { k: 4, a: 7, b: 8, start: '14:15', end: '15:50' },
  { k: 5, a: 9, b: 10, start: '16:00', end: '17:35' },
  { k: 6, a: 11, b: 12, start: '19:00', end: '20:35' },
];
// h = hue for the low-chroma course palette (seal-red range 15–35 kept free)
const COURSES = [
  { name: '机器学习基础', room: 'JB406', teacher: '陈思远', day: 1, slot: 1, w: [5, 16], h: 250 },
  { name: '中华诗词鉴赏', room: 'JB112', teacher: '李文博', day: 1, slot: 2, w: [1, 4], h: 70 },
  { name: '电路分析 B', room: 'JB223', teacher: '吴海峰', day: 1, slot: 3, w: [1, 16], h: 150 },
  { name: '大学英语 3', room: 'JB223', teacher: '林雪', day: 1, slot: 5, w: [1, 14], h: 320 },
  { name: '机器学习基础', room: '5208', teacher: '陈思远', day: 2, slot: 1, w: [5, 16], h: 250, note: '智能计算实验室' },
  { name: '软件需求与建模', room: 'JB320', teacher: '郑雅琴', day: 2, slot: 2, w: [1, 16], h: 95 },
  { name: '软件需求与建模', room: '4106', teacher: '郑雅琴', day: 2, slot: 4, w: [1, 16], h: 95, note: '软件工程实验室' },
  { name: '操作系统原理', room: 'JB411', teacher: '黄志明', day: 3, slot: 1, w: [1, 16], h: 285 },
  { name: '计算机网络', room: 'JB305', teacher: '马晓东', day: 3, slot: 2, w: [9, 16], h: 45 },
  { name: '模拟电子实验', room: '电子技术实验室', teacher: '宋佳', day: 4, slot: 1, w: [3, 14], h: 185 },
  { name: '概率统计 B', room: 'JB320', teacher: '唐毅', day: 4, slot: 2, w: [1, 16], h: 215 },
  { name: '电影艺术欣赏', room: 'JB203', teacher: '白露', day: 4, slot: 3, w: [5, 12], h: 350 },
  { name: 'Java 程序设计', room: 'JA118', teacher: '冯博', day: 4, slot: 4, w: [9, 16], h: 60 },
  { name: '计算机网络', room: 'JB305', teacher: '马晓东', day: 5, slot: 1, w: [9, 16], h: 45 },
  { name: 'Java 程序设计', room: 'JA118', teacher: '冯博', day: 5, slot: 2, w: [1, 16], h: 60 },
  { name: '羽毛球 3', room: '体育馆', teacher: '韩磊', day: 5, slot: 4, w: [1, 16], h: 125 },
  { name: '概率统计 B', room: 'JB208', teacher: '唐毅', day: 6, slot: 2, w: [17, 18], h: 215, note: '期末答疑' },
];COURSES.forEach((c, i) => c.id = i);
const TERM_START = new Date(2026, 8, 7); // 第 1 周周一
const TODAY = { week: 7, day: 1 };
const WD = ['一', '二', '三', '四', '五', '六', '日'];

const TOOLS = {
  pickup: { name: '淘宝取件', sub: '驿站取件码', h: 50, val: '2 件待取', hot: true, short: '取件', st: '2 件', badge: 2,
            ic: '<path d="M3.5 7.5 12 3.5l8.5 4v9L12 20.5l-8.5-4z"/><path d="M3.5 7.5 12 11.5l8.5-4M12 11.5v9"/>' },
  power:  { name: '电费查询', sub: '宿舍剩余电量', h: 95, val: '38.6 度', short: '电费', st: '38.6 度',
            ic: '<path d="M13 2.5 5 13.5h6l-1 8 8-11h-6z"/>' },
  todo:   { name: '超星待办', sub: '作业与考试', h: 255, val: '3 项', hot: true, short: '待办', st: '3 项', badge: 3,
            ic: '<path d="M4.5 5A1.5 1.5 0 0 1 6 3.5h13.5v14H6a1.5 1.5 0 0 0-1.5 1.5z"/><path d="M4.5 19A1.5 1.5 0 0 0 6 20.5h13.5v-3"/><path d="m9 10.5 2 2 4-4"/>' },
};
const CHEV_R = '<svg class="i" viewBox="0 0 24 24"><path d="m9.5 6 6 6-6 6"/></svg>';
const CHEV_L = '<svg class="i" viewBox="0 0 24 24"><path d="m14.5 6-6 6 6 6"/></svg>';
const ico = (paths) => `<svg class="i" viewBox="0 0 24 24">${paths}</svg>`;

/* ---------------- helpers ---------------- */
const toMin = (t) => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
const fmt = (m) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;
const slotOf = (c) => SLOTS[c.slot - 1];
const inWeek = (c, w) => w >= c.w[0] && w <= c.w[1];
const dateOf = (week, day) => { const d = new Date(TERM_START); d.setDate(d.getDate() + (week - 1) * 7 + day - 1); return d; };
const md = (d) => `${d.getMonth() + 1}月${d.getDate()}日`;
const coursesOn = (week, day) => COURSES.filter(c => c.day === day && inWeek(c, week)).sort((a, b) => a.slot - b.slot);
const esc = (s) => s.replace(/[&<>"]/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[ch]));

const state = { now: 642, theme: 'light', selDay: TODAY.day, week: TODAY.week };

/* what the hero should show at state.now */
function heroModel() {
  const list = coursesOn(TODAY.week, TODAY.day);
  const now = state.now;
  for (const c of list) {
    const s = slotOf(c), st = toMin(s.start), en = toMin(s.end);
    if (now >= st && now < en) return { kind: 'now', c, left: en - now };
    if (now < st) return { kind: 'next', c, wait: st - now };
  }
  const tmr = coursesOn(TODAY.week, TODAY.day + 1)[0];
  return { kind: 'done', c: tmr };
}

function durationHTML(m) {
  const h = Math.floor(m / 60), mm = m % 60;
  if (h === 0) return `<span class="num">${mm}</span><small>分钟</small>`;
  return `<span class="num">${h}</span><small>小时</small>${mm ? `<span class="num">${mm}</span><small>分</small>` : ''}`;
}

const SEAL = `<svg class="seal-wm" viewBox="0 0 200 200" aria-hidden="true">
  <defs><path id="arcT" d="M26 100a74 74 0 0 1 148 0"/><path id="arcB" d="M18 100a82 82 0 0 0 164 0"/></defs>
  <circle cx="100" cy="100" r="97" fill="none" stroke="currentColor" stroke-width="4"/>
  <circle cx="100" cy="100" r="58" fill="none" stroke="currentColor" stroke-width="2.5"/>
  <text fill="currentColor" font-size="22" font-weight="700" letter-spacing="6"><textPath href="#arcT" startOffset="50%" text-anchor="middle">齐鲁理工学院</textPath></text>
  <text fill="currentColor" font-size="13" font-weight="700" letter-spacing="1.5" font-family="system-ui"><textPath href="#arcB" startOffset="50%" text-anchor="middle" dominant-baseline="hanging">QILU INSTITUTE OF TECHNOLOGY</textPath></text>
</svg>`;

/* ---------------- home ---------------- */
function renderHome(el) {
  const m = heroModel();
  const today = dateOf(TODAY.week, TODAY.day);
  const list = coursesOn(TODAY.week, TODAY.day);
  const dayStart = toMin(slotOf(list[0]).start) - 30, dayEnd = toMin(slotOf(list[list.length - 1]).end) + 30;
  const pct = (t) => Math.max(0, Math.min(100, (t - dayStart) / (dayEnd - dayStart) * 100));

  let top, count;
  const s = m.c && slotOf(m.c);
  if (m.kind === 'next') {
    top = `<span>下一节</span><span class="num">${s.start} – ${s.end}</span>`;
    count = `${durationHTML(m.wait)}<span class="tail">后上课</span>`;
  } else if (m.kind === 'now') {
    top = `<span>正在上课</span><span class="num">${s.start} – ${s.end}</span>`;
    count = `<span class="tail" style="margin:0 6px 0 0">还剩</span>${durationHTML(m.left)}`;
  } else {
    top = `<span>今天的课上完了，明天第一节</span><span class="num">周${WD[TODAY.day]}</span>`;
    count = `<span class="num">${s.start}</span><span class="tail">开始</span>`;
  }
  const segs = list.map(c => { const q = slotOf(c); const a = pct(toMin(q.start)), b = pct(toMin(q.end)); return `<i class="seg-c" style="left:${a}%;width:${b - a}%"></i>`; }).join('');
  const nowPct = pct(state.now);

  const hero = `<article class="hero" style="--h:${m.c.h}" data-course="${m.c.id}" tabindex="0" role="button">
      ${SEAL}
      <div class="hero-top">${top}</div>
      <h2>${esc(m.c.name)}</h2>
      <div class="hero-where"><span class="num">${esc(m.c.room)}</span><span>${esc(m.c.teacher)}</span></div>
      <div class="hero-count">${count}</div>
      ${m.kind !== 'done' ? `<div class="track">${segs}<i class="fill" style="width:${nowPct}%;opacity:.0"></i><i class="now" style="left:${nowPct}%"></i></div>
      <div class="track-scale num"><span>${fmt(dayStart + 30)}</span><span>${fmt(dayEnd - 30)}</span></div>` : ''}
    </article>`;

  const strip = WD.map((w, i) => {
    const d = i + 1, n = coursesOn(TODAY.week, d).length, dt = dateOf(TODAY.week, d);
    const cls = [d === TODAY.day && 'is-today', d === state.selDay && 'is-sel'].filter(Boolean).join(' ');
    return `<button class="${cls}" data-day="${d}" aria-pressed="${d === state.selDay}" aria-label="周${w} ${n} 节课">
        <span class="wd">${w}</span><span class="dn">${dt.getDate()}</span>
        <span class="dots">${'<i></i>'.repeat(Math.min(n, 4))}</span></button>`;
  }).join('');

  const isToday = state.selDay === TODAY.day;
  const dayList = coursesOn(TODAY.week, state.selDay);
  const selDate = dateOf(TODAY.week, state.selDay);
  let tl = '';
  dayList.forEach((c, i) => {
    const q = slotOf(c), st = toMin(q.start), en = toMin(q.end);
    if (i > 0) {
      const prevEnd = toMin(slotOf(dayList[i - 1]).end), gap = st - prevEnd;
      if (gap >= 60) tl += `<li class="gap" aria-hidden="true"><span></span><span class="rail"></span><span>空 ${Math.floor(gap / 60)} 小时${gap % 60 ? ` ${gap % 60} 分` : ''}</span></li>`;
    }
    let cls = '';
    if (isToday) {
      if (state.now >= en) cls = 'is-done';
      else if (state.now >= st) cls = 'is-now';
      else if (m.kind === 'next' && m.c === c) cls = 'is-next';
    }
    tl += `<li class="tl ${cls}" style="--h:${c.h}" data-course="${c.id}" tabindex="0">
        <span class="t"><b>${q.start}</b><span>${q.end}</span></span>
        <span class="rail"><i class="dot"></i></span>
        <span class="body"><b>${esc(c.name)}</b><span>${esc(c.teacher)}</span></span>
        <span class="room">${esc(c.room)}</span></li>`;
  });
  const remaining = isToday ? dayList.filter(c => state.now < toMin(slotOf(c).end)).length : dayList.length;
  const headRight = isToday ? (remaining ? `还有 ${remaining} 节` : '全部结束') : `${dayList.length} 节`;

  const sc = ['pickup', 'power', 'todo'].map(k => { const t = TOOLS[k];
    return `<button data-tool="${k}"><span class="ticon" style="--h:${t.h}">${ico(t.ic)}${t.badge ? `<span class="badge">${t.badge}</span>` : ''}</span>
      <span class="lb">${t.short}</span><span class="st">${t.st}</span></button>`; }).join('');

  el.innerHTML = `
    <div class="topbar">
      <div><div class="date">${md(today)} 周${WD[TODAY.day - 1]}</div><div class="sub">第 ${TODAY.week} 周</div></div>
      <button class="avatar" data-me aria-label="个人页">林</button>
    </div>
    ${hero}
    <div class="week-strip">${strip}</div>
    <div class="day-head"><h3>${isToday ? '今天' : `周${WD[state.selDay - 1]} ${md(selDate)}`}</h3><span>${headRight}</span></div>
    ${dayList.length ? `<ol class="timeline">${tl}</ol>` : `<div class="empty-day">这天没有课</div>`}
    <div class="shortcuts">${sc}</div>`;
}

/* ---------------- week grid ---------------- */
function renderWeek(el, dir) {
  const w = state.week;
  const real = COURSES.filter(c => inWeek(c, w));
  const showSat = real.some(c => c.day === 6), showSun = real.some(c => c.day === 7);
  const days = [1, 2, 3, 4, 5].concat(showSat || showSun ? [6] : [], showSun ? [7] : []);
  const isCur = w === TODAY.week;
  const a = dateOf(w, 1), b = dateOf(w, 7);

  const head = `<div class="wk-head">
      <div><h2>第<span class="num">${w}</span>周</h2><p>${md(a)} – ${b.getMonth() === a.getMonth() ? b.getDate() + '日' : md(b)}${isCur ? '，本周' : ''}</p></div>
      <div class="wk-nav">
        ${isCur ? '' : '<button class="back" data-week="cur">回到本周</button>'}
        <button class="ic" data-week="-1" aria-label="上一周" ${w <= 1 ? 'disabled' : ''}>${CHEV_L}</button>
        <button class="ic" data-week="1" aria-label="下一周" ${w >= 18 ? 'disabled' : ''}>${CHEV_R}</button>
      </div></div>`;

  const dayHead = `<div class="days" style="--cols:${days.length}"><span></span>${days.map(d => {
      const t = isCur && d === TODAY.day;
      return `<div class="${t ? 'is-today' : ''}">${WD[d - 1]}<b>${dateOf(w, d).getDate()}</b></div>`; }).join('')}</div>`;

  let cells = '';
  SLOTS.forEach(s => {
    cells += `<div class="sec"><span>${s.a}</span><span>${s.b}</span></div>`;
    days.forEach(d => {
      const here = COURSES.filter(c => c.day === d && c.slot === s.k);
      const on = here.find(c => inWeek(c, w)), off = here.find(c => !inWeek(c, w));
      const today = isCur && d === TODAY.day;
      let inner = '';
      if (on) inner = `<button class="cblk" style="--h:${on.h}" data-course="${on.id}"><b>${esc(on.name)}</b><span>${esc(on.room)}</span></button>`;
      else if (off) inner = `<button class="cblk ghost" data-course="${off.id}" aria-label="${esc(off.name)}，本周不上"><b>${esc(off.name)}</b></button>`;
      cells += `<div class="cell${today ? ' today' : ''}">${inner}</div>`;
    });
  });

  // red now-line, only in the current week and inside teaching hours
  let line = '';
  if (isCur) {
    const ROW = 104, GAP = 4, n = state.now;
    let y = null;
    SLOTS.forEach((s, i) => {
      const st = toMin(s.start), en = toMin(s.end), top = i * (ROW + GAP);
      if (n >= st && n < en) y = top + (n - st) / (en - st) * ROW;
      const next = SLOTS[i + 1];
      if (next && n >= en && n < toMin(next.start)) y = top + ROW + GAP / 2;
    });
    if (y !== null) {
      const col = days.indexOf(TODAY.day);
      line = `<div class="nowline" style="top:${y}px" data-t="${fmt(n)}"><i style="left:calc((100% - ${(days.length - 1) * 4}px) / ${days.length} * ${col} + ${col * 4}px)"></i></div>`;
    }
  }

  const hasGhost = days.some(d => SLOTS.some(s => COURSES.some(c => c.day === d && c.slot === s.k && !inWeek(c, w)) && !COURSES.some(c => c.day === d && c.slot === s.k && inWeek(c, w))));
  const hidden = !days.includes(7) ? (days.includes(6) ? '周日' : '周末') : '';
  el.innerHTML = `${head}
    <div class="grid-wrap${dir ? (dir > 0 ? ' anim-l' : ' anim-r') : ''}">
      ${dayHead}
      <div class="grid" style="--cols:${days.length}">${cells}${line}</div>
      <div class="wk-foot">${hasGhost ? '<i></i><span>虚线是本周不上的课</span>' : ''}${hidden ? `<span style="margin-left:auto">${hidden}没课，已收起</span>` : ''}</div>
    </div>`;
}

/* ---------------- tools ---------------- */
function renderTools(el) {
  const row = (k) => { const t = TOOLS[k];
    return `<button class="row" data-tool="${k}"><span class="ticon" style="--h:${t.h}">${ico(t.ic)}</span>
      <span class="tx"><b>${t.name}</b><span>${t.sub}</span></span>
      ${t.val ? `<span class="val${t.hot ? ' hot' : ''}">${t.val}</span>` : ''}${CHEV_R}</button>`; };
  el.innerHTML = `<h1 class="page-title">工具</h1>
    <section class="group"><h3>校园生活</h3><div class="list">${['pickup', 'power'].map(row).join('')}</div></section>
    <section class="group"><h3>学习</h3><div class="list">${row('todo')}</div></section>`;
}

/* ---------------- sheets ---------------- */
function courseSheet(c) {
  const s = slotOf(c);
  const thisWeek = inWeek(c, state.week);
  return `<div class="grab"></div>
    <span class="chip" style="--h:${c.h}">${thisWeek ? `周${WD[c.day - 1]}` : '本周不上'}</span>
    <h2>${esc(c.name)}</h2>
    <dl>
      <dt>时间</dt><dd>周${WD[c.day - 1]} 第 ${s.a}–${s.b} 节　<span class="num">${s.start} – ${s.end}</span></dd>
      <dt>地点</dt><dd><span class="num">${esc(c.room)}</span>${c.note ? `　${esc(c.note)}` : ''}</dd>
      <dt>教师</dt><dd>${esc(c.teacher)}</dd>
      <dt>周次</dt><dd>第 <span class="num">${c.w[0]}–${c.w[1]}</span> 周</dd>
    </dl>
    <div class="actions"><button data-close>编辑课程</button><button data-close>关闭</button></div>`;
}
function meSheet() {
  const r = (h, ic, name, val) => `<button class="row" data-close><span class="ticon" style="--h:${h}">${ico(ic)}</span><span class="tx"><b>${name}</b></span>${val ? `<span class="val">${val}</span>` : ''}${CHEV_R}</button>`;
  return `<div class="grab"></div>
    <div class="me"><span class="avatar">林</span><div><b>林一舟</b><span>课表导入于 10月19日 00:22</span></div></div>
    <div class="list">
      ${r(250, '<circle cx="12" cy="12" r="3"/><path d="M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M5.6 18.4l1.8-1.8M16.6 7.4l1.8-1.8"/>', '设置', '')}
      ${r(150, '<path d="M19.5 11a7.5 7.5 0 0 0-13.4-4.6L4.5 8.5"/><path d="M4.5 4v4.5H9"/><path d="M4.5 13a7.5 7.5 0 0 0 13.4 4.6l1.6-2.1"/><path d="M19.5 20v-4.5H15"/>', '检查更新', 'V3.0.0')}
      ${r(60, '<path d="M4.5 6A1.5 1.5 0 0 1 6 4.5h12A1.5 1.5 0 0 1 19.5 6v9a1.5 1.5 0 0 1-1.5 1.5H9.5l-5 4z"/>', '意见反馈', '')}
      ${r(320, '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.6v.2"/>', '关于 QLIT 小助手', '')}
    </div>`;
}

/* ================= site glue ================= */
// Same feed the app's update checker reads. FALLBACK_APK always 302s to the live APK,
// so the button and QR still work if the feed can't be fetched.
const UPDATE_URL = 'https://api.kevinxu.cn/update.json';
const FALLBACK_APK = 'https://api.kevinxu.cn/latest.apk';

const phones = [];
function mountPhone(host, tab) {
  host.append(document.getElementById('phoneTpl').content.cloneNode(true));
  const el = host.querySelector('.phone');
  const p = { el, tab, screens: {} };
  el.querySelectorAll('.screen').forEach(s => p.screens[s.dataset.screen] = s);
  phones.push(p);

  el.addEventListener('click', (e) => {
    const t = e.target.closest('button, [data-course], [data-me]');
    if (!t || !el.contains(t) || t.closest('.scrim')) return;
    if (t.dataset.tab) return setTab(p, t.dataset.tab);
    if (t.dataset.day) { state.selDay = +t.dataset.day; return renderAll(); }
    if (t.dataset.week) { changeWeek(t.dataset.week === 'cur' ? TODAY.week - state.week : +t.dataset.week); return; }
    if (t.hasAttribute('data-close')) return closeSheet(p);
    if (t.hasAttribute('data-me')) return openSheet(p, meSheet());
    if (t.dataset.tool) return;
    if (t.dataset.course !== undefined && !t.closest('.sheet')) return openSheet(p, courseSheet(COURSES[+t.dataset.course]));
  });
  el.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeSheet(p);
    if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('[data-course][tabindex]')) { e.preventDefault(); e.target.click(); }
  });
  el.querySelector('.scrim').addEventListener('click', () => closeSheet(p));

  let sx = null, sy = null;
  p.screens.week.addEventListener('pointerdown', (e) => { if (e.target.closest('.grid-wrap')) { sx = e.clientX; sy = e.clientY; } });
  p.screens.week.addEventListener('pointerup', (e) => {
    if (sx === null) return;
    const dx = e.clientX - sx, dy = e.clientY - sy; sx = null;
    if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) changeWeek(dx < 0 ? 1 : -1);
  });
  setTab(p, tab);
}
function setTab(p, tab) {
  p.tab = tab;
  Object.entries(p.screens).forEach(([k, s]) => s.hidden = k !== tab);
  const idx = ['home', 'week', 'tools'].indexOf(tab);
  p.el.querySelector('.pill').style.transform = `translateX(${idx * 78}px)`;
  p.el.querySelectorAll('.tabbar button').forEach(b => b.dataset.tab === tab ? b.setAttribute('aria-current', 'page') : b.removeAttribute('aria-current'));
}
function openSheet(p, html) {
  const sh = p.el.querySelector('.sheet');
  sh.innerHTML = html;
  p.el.classList.add('sheet-open');
  sh.querySelector('button')?.focus({ preventScroll: true });
}
function closeSheet(p) { p.el.classList.remove('sheet-open'); }
function changeWeek(d) {
  const nw = Math.max(1, Math.min(18, state.week + d));
  if (nw === state.week) return;
  state.week = nw;
  phones.forEach(p => renderWeek(p.screens.week, d));
}
function renderAll() {
  phones.forEach(p => {
    renderHome(p.screens.home);
    renderWeek(p.screens.week, 0);
    renderTools(p.screens.tools);
    p.el.querySelector('.st-time').textContent = fmt(state.now);
  });
  document.getElementById('nowLabel').textContent = fmt(state.now);
  document.getElementById('typeNum').textContent = fmt(state.now);
}

/* ---- clock: follow the real time while it falls inside class hours ---- */
const clock = document.getElementById('clock'), clockOut = document.getElementById('clockOut');
let followReal = true;
function realMinutes() { const d = new Date(); return d.getHours() * 60 + d.getMinutes(); }
function syncClock() {
  if (!followReal) return;
  const m = realMinutes();
  state.now = m >= 480 && m < 1055 ? m : 642;
  clock.value = state.now; clockOut.textContent = fmt(state.now);
  renderAll();
}
clock.addEventListener('input', () => { followReal = false; state.now = +clock.value; clockOut.textContent = fmt(state.now); renderAll(); });
setInterval(syncClock, 30000);

/* ---- theme ---- */
const root = document.documentElement, themeBtn = document.getElementById('themeBtn');
const SUN = '<svg class="i" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/></svg>';
const MOON = '<svg class="i" viewBox="0 0 24 24"><path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/></svg>';
function applyTheme(t, save) {
  root.dataset.theme = t;
  themeBtn.innerHTML = t === 'dark' ? SUN : MOON;
  themeBtn.setAttribute('aria-label', t === 'dark' ? '切换浅色模式' : '切换深色模式');
  document.querySelector('meta[name="theme-color"]').content = t === 'dark' ? '#141312' : '#F3F1EC';
  if (save) try { localStorage.setItem('qlit-theme', t); } catch (e) {}
}
themeBtn.addEventListener('click', () => applyTheme(root.dataset.theme === 'dark' ? 'light' : 'dark', true));
(() => {
  let saved = null; try { saved = localStorage.getItem('qlit-theme'); } catch (e) {}
  const mq = matchMedia('(prefers-color-scheme: dark)');
  applyTheme(saved || (mq.matches ? 'dark' : 'light'), false);
  mq.addEventListener?.('change', (e) => { let s = null; try { s = localStorage.getItem('qlit-theme'); } catch (_) {} if (!s) applyTheme(e.matches ? 'dark' : 'light', false); });
})();

/* ---- static specimens built from the same data as the phones ---- */
document.getElementById('specStrip').innerHTML = WD.map((w, i) => {
  const d = dateOf(TODAY.week, i + 1);
  return `<span class="${i + 1 === TODAY.day ? 'is-today' : ''}">${w}<b>${d.getDate()}</b></span>`;
}).join('');
document.getElementById('specTools').innerHTML = ['pickup', 'power', 'todo'].map(k => {
  const t = TOOLS[k];
  return `<span class="ticon" style="--h:${t.h}">${ico(t.ic)}${t.badge ? `<span class="badge">${t.badge}</span>` : ''}</span>`;
}).join('');
document.getElementById('toolList').innerHTML = ['pickup', 'power', 'todo'].map(k => {
  const t = TOOLS[k];
  return `<div class="s-tool"><span class="ticon" style="--h:${t.h}">${ico(t.ic)}</span><div><b>${t.name}</b><span>${t.sub}</span></div></div>`;
}).join('');
const seen = new Set();
document.getElementById('hues').innerHTML = COURSES.filter(c => !seen.has(c.h) && seen.add(c.h)).sort((a, b) => a.h - b.h)
  .map(c => `<div class="cblk" style="--h:${c.h}"><b>${esc(c.name)}</b><span>H ${c.h}°</span></div>`).join('');
document.getElementById('swatches').innerHTML = [
  ['--page', '底'], ['--bg', '纸'], ['--surface', '面'], ['--ink', '墨'], ['--seal', '印'],
].map(([v, n]) => `<span class="swatch"><i style="background:var(${v})"></i>${n}<code data-var="${v}"></code></span>`).join('');
function paintSwatchCodes() {
  document.querySelectorAll('.swatch code').forEach(c => c.textContent = getComputedStyle(root).getPropertyValue(c.dataset.var).trim().toUpperCase());
}
new MutationObserver(paintSwatchCodes).observe(root, { attributes: true, attributeFilter: ['data-theme'] });
paintSwatchCodes();
document.getElementById('dlSeal').innerHTML = SEAL.replace(/^<svg[^>]*>|<\/svg>$/g, '').replace(/arcT/g, 'dlArcT').replace(/arcB/g, 'dlArcB');
document.getElementById('year').textContent = Math.max(2026, new Date().getFullYear());

/* ---- download link, version and QR ---- */
function qrSvg(text) {
  const q = qrcode(0, 'M'); q.addData(text); q.make();
  const n = q.getModuleCount(); let d = '';
  for (let r = 0; r < n; r++) for (let c = 0; c < n; c++) if (q.isDark(r, c)) d += `M${c} ${r}h1v1h-1z`;
  return `<svg viewBox="0 0 ${n} ${n}" shape-rendering="crispEdges"><path d="${d}" fill="#1D1B18"/></svg>`;
}
function setDownload(url, ver) {
  document.querySelectorAll('[data-apk]').forEach(a => { a.href = url; a.target = url.startsWith('#') ? '' : '_blank'; a.rel = 'noopener'; });
  if (ver) document.querySelectorAll('[data-ver]').forEach(s => s.textContent = ver);
}
try { document.getElementById('qr').innerHTML = qrSvg(FALLBACK_APK); }
catch (e) { document.querySelector('.s-qr').hidden = true; }
setDownload(FALLBACK_APK);
(async () => {
  try {
    const ctl = new AbortController(); const tm = setTimeout(() => ctl.abort(), 4000);
    const r = await fetch(UPDATE_URL, { signal: ctl.signal, cache: 'no-store' }); clearTimeout(tm);
    const j = await r.json();
    if (j && j.apkUrl) setDownload(j.apkUrl, j.versionName);
  } catch (e) { /* keep fallback */ }
})();

/* ---- nav border + reveal ---- */
const nav = document.getElementById('nav');
addEventListener('scroll', () => nav.classList.toggle('is-stuck', scrollY > 8), { passive: true });
if ('IntersectionObserver' in window) {
  const io = new IntersectionObserver((es) => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } }), { rootMargin: '0px 0px -8% 0px' });
  document.querySelectorAll('.rv').forEach(el => io.observe(el));
} else document.querySelectorAll('.rv').forEach(el => el.classList.add('in'));

/* ---- boot ---- */
document.querySelectorAll('[data-phone]').forEach(h => mountPhone(h, h.dataset.phone));
syncClock();
renderAll();
