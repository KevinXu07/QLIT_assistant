'use strict';

// 视觉与 App「校徽红」重设计一致：暖白纸面、墨色控件、校徽红只标“此刻”和需要处理的事。
// 整页是一个模板字符串：内联脚本里不要出现反引号和美元符加花括号。
function adminPageHtml() {
  return `<!doctype html>
<html lang="zh-CN" data-theme="light">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>QLIT 发布中心</title>
<script>
  (function () {
    var t = null;
    try { t = localStorage.getItem('qlit_admin_theme'); } catch (e) {}
    if (!t) t = window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    document.documentElement.dataset.theme = t;
  })();
</script>
<style>
  :root {
    --num: "Barlow Semi Condensed", "DIN Alternate", Bahnschrift, system-ui, sans-serif;
    --cjk: system-ui, -apple-system, "PingFang SC", "MiSans", "HarmonyOS Sans SC", "Noto Sans SC", "Microsoft YaHei", sans-serif;
    --mono: ui-monospace, "SF Mono", "Cascadia Code", Consolas, monospace;
    --ease-out: cubic-bezier(.32, .72, 0, 1);
  }
  [data-theme="light"] {
    color-scheme: light;
    --page: #E3E0DA; --bg: #F3F1EC; --surface: #FFFFFF; --surface-2: #EBE8E2; --field: #F8F7F4;
    --ink: #1D1B18; --ink-2: #6B665E; --ink-3: #A39D93;
    --line: rgba(29, 27, 24, .08); --line-2: rgba(29, 27, 24, .15);
    --seal: #C8161D; --seal-soft: rgba(200, 22, 29, .1);
    --shadow: 0 1px 2px rgba(60, 40, 20, .05), 0 10px 30px -14px rgba(60, 40, 20, .16);
    --cb-l: .935; --cb-c: .042; --cr-l: .64; --cr-c: .13; --ct-l: .36; --ct-c: .085;
    --chev: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%236B665E' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  }
  [data-theme="dark"] {
    color-scheme: dark;
    --page: #0A0A09; --bg: #141312; --surface: #1F1D1B; --surface-2: #2B2825; --field: #191816;
    --ink: #F2EFEA; --ink-2: #A8A29A; --ink-3: #6E6962;
    --line: rgba(255, 255, 255, .07); --line-2: rgba(255, 255, 255, .13);
    --seal: #CC2A31; --seal-soft: rgba(230, 70, 76, .16);
    --shadow: 0 1px 2px rgba(0, 0, 0, .3), 0 10px 30px -14px rgba(0, 0, 0, .6);
    --cb-l: .31; --cb-c: .05; --cr-l: .72; --cr-c: .12; --ct-l: .9; --ct-c: .05;
    --chev: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23A8A29A' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  }

  * { box-sizing: border-box; }
  html, body { min-height: 100%; }
  body {
    margin: 0; color: var(--ink); background: var(--bg);
    font: 14px/1.55 var(--cjk); -webkit-font-smoothing: antialiased;
    transition: background-color .3s, color .3s;
  }
  button, input, textarea, select { font: inherit; color: inherit; }
  button { cursor: pointer; }
  button:disabled { cursor: not-allowed; opacity: .5; }
  button:active:not(:disabled) { transform: scale(.97); }
  button, .upload-box, .nav button, .bar-fill, .check, input, textarea, select {
    transition-duration: 200ms; transition-timing-function: var(--ease-out);
  }
  button:focus-visible, a:focus-visible, .trend-column:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
  .hidden { display: none !important; }
  ::selection { background: var(--ink); color: var(--bg); }
  .num { font-family: var(--num); font-variant-numeric: tabular-nums; }
  .icon { width: 18px; height: 18px; flex: none; vertical-align: -4px; }

  /* ── 品牌印章 ── */
  .brand-mark { width: 40px; height: 40px; flex: none; display: block; }
  .seal-wm { position: absolute; pointer-events: none; color: #fff; opacity: .13; }

  .icon-btn {
    width: 38px; height: 38px; display: grid; place-items: center;
    border: 0; border-radius: 50%; background: transparent; color: var(--ink-2);
    transition-property: background, color, transform;
  }
  .icon-btn:hover { background: var(--line); color: var(--ink); }
  .icon-btn .icon { width: 19px; height: 19px; }

  /* ── 登录 ── */
  .auth-shell { position: relative; min-height: 100vh; display: grid; place-items: center; padding: 24px; }
  .auth-shell > .icon-btn { position: absolute; top: 18px; right: 18px; }
  .auth-card {
    width: min(400px, 100%); padding: 36px 32px 32px;
    background: var(--surface); border-radius: 32px; box-shadow: var(--shadow);
    animation: rise .5s var(--ease-out) both;
  }
  .auth-card .brand-mark { width: 52px; height: 52px; }
  .auth-card h1 { margin: 22px 0 6px; font-size: 26px; font-weight: 700; }
  .auth-card p { margin: 0 0 26px; color: var(--ink-2); font-size: 14px; }

  /* ── 框架 ── */
  .app-shell { min-height: 100vh; display: grid; grid-template-columns: 232px minmax(0, 1fr); }
  .sidebar { display: flex; flex-direction: column; padding: 24px 14px; border-right: 1px solid var(--line); }
  .sidebar-brand { display: flex; align-items: center; gap: 11px; padding: 0 8px 30px; }
  .sidebar-brand .brand-mark { width: 34px; height: 34px; }
  .sidebar-brand strong { display: block; font-size: 15px; font-weight: 650; }
  .sidebar-brand span { display: block; margin-top: 1px; color: var(--ink-3); font-size: 12px; }
  .nav { display: grid; gap: 4px; }
  .nav button {
    width: 100%; display: flex; align-items: center; gap: 10px; padding: 10px 14px;
    border: 0; border-radius: 999px; background: transparent; color: var(--ink-2);
    text-align: left; font-size: 14px; font-weight: 550;
    transition-property: background, color, box-shadow, transform;
  }
  .nav button:hover { color: var(--ink); background: var(--line); }
  .nav button.active { color: var(--ink); background: var(--surface); box-shadow: var(--shadow); font-weight: 650; }
  .sidebar-footer { margin-top: auto; padding: 20px 8px 0; color: var(--ink-3); font-size: 12px; }
  .sidebar-footer strong { display: block; color: var(--ink-2); font-weight: 600; }

  .content { min-width: 0; max-width: 1180px; padding: 26px clamp(20px, 4vw, 52px) 64px; }
  .topbar { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 30px; }
  .topbar h1 { margin: 0; font-size: 28px; font-weight: 700; line-height: 1.2; }
  .topbar p { margin: 4px 0 0; color: var(--ink-3); font-size: 13px; font-variant-numeric: tabular-nums; }
  .topbar-actions { display: flex; align-items: center; gap: 8px; }
  .status-chip {
    display: inline-flex; align-items: center; gap: 8px; padding: 6px 13px;
    border-radius: 999px; background: var(--surface); color: var(--ink-2);
    box-shadow: inset 0 0 0 1px var(--line); font-size: 13px; font-weight: 550;
  }
  .status-dot { width: 7px; height: 7px; border-radius: 50%; background: oklch(var(--cr-l) var(--cr-c) 150); }
  .status-chip[data-state="error"] { color: var(--seal); background: var(--seal-soft); box-shadow: none; }
  .status-chip[data-state="error"] .status-dot { background: var(--seal); }
  .status-chip[data-state="loading"] .status-dot { background: var(--ink-3); animation: blink 1.1s ease-in-out infinite; }

  .page-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
  .page-heading h2 { margin: 0; font-size: 19px; font-weight: 650; }
  .page-heading p { margin: 3px 0 0; color: var(--ink-2); font-size: 13px; }

  .card { padding: 24px; background: var(--surface); border-radius: 28px; }
  .card + .card { margin-top: 16px; }
  .card-title { display: flex; align-items: start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
  .card-title h3 { margin: 0; font-size: 17px; font-weight: 650; }
  .card-title p { margin: 3px 0 0; color: var(--ink-2); font-size: 13px; }

  /* ── 线上版本：唯一一块校徽红，代表“此刻”线上的那个包 ── */
  .current-release {
    position: relative; overflow: hidden; margin-bottom: 16px; padding: 20px 24px 22px;
    border-radius: 28px; background: var(--seal); color: #fff;
    box-shadow: 0 14px 30px -14px rgba(200, 22, 29, .55);
  }
  [data-theme="dark"] .current-release { box-shadow: none; }
  .current-release .seal-wm { width: 230px; height: 230px; right: -60px; top: -50px; }
  .current-release.is-empty { background: var(--surface); color: var(--ink); box-shadow: none; border: 1.5px dashed var(--line-2); }
  .current-release.is-empty .seal-wm { color: var(--ink); opacity: .05; }
  .cr-top { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; opacity: .85; }
  .cr-ver { display: flex; align-items: baseline; gap: 10px; margin: 8px 0 6px; }
  .cr-ver b { font-family: var(--num); font-size: 46px; font-weight: 600; line-height: 1; letter-spacing: .01em; }
  .cr-ver small { font-family: var(--num); font-size: 18px; font-weight: 600; opacity: .8; }
  .cr-ver.is-empty b { font-family: var(--cjk); font-size: 24px; font-weight: 700; }
  .cr-meta { font-size: 13px; opacity: .85; overflow-wrap: anywhere; }
  .cr-meta .num { font-size: 15px; font-weight: 600; opacity: 1; }
  .release-notes { margin: 0 0 16px; padding: 4px 20px; border-radius: 22px; background: var(--surface); color: var(--ink-2); font-size: 14px; }
  .release-notes summary { padding: 12px 0; cursor: pointer; font-weight: 600; color: var(--ink); }
  .release-notes pre { margin: 0 0 16px; white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.75; }

  /* ── 表单 ── */
  .publish-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(260px, .65fr); gap: 16px; align-items: start; }
  .field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  fieldset { min-width: 0; margin: 0; padding: 0; border: 0; }
  fieldset:disabled .upload-box { opacity: .55; }
  .field { display: grid; gap: 7px; }
  .field + .field { margin-top: 14px; }
  .field-grid .field + .field { margin-top: 0; }
  label { color: var(--ink-2); font-size: 13px; font-weight: 600; }
  input, textarea, select {
    width: 100%; color: var(--ink); background: var(--field);
    border: 1px solid var(--line-2); border-radius: 14px; outline: none; padding: 11px 14px;
    transition-property: border-color, box-shadow, background;
  }
  input::placeholder, textarea::placeholder { color: var(--ink-3); }
  input:focus, textarea:focus, select:focus { border-color: var(--ink); background: var(--surface); box-shadow: 0 0 0 4px var(--line); }
  input[type="number"] { font-family: var(--num); font-size: 16px; font-weight: 600; }
  input[type="number"]::placeholder { font-family: var(--cjk); font-size: 14px; font-weight: 400; }
  textarea { min-height: 140px; resize: vertical; line-height: 1.7; }
  select { appearance: none; padding-right: 36px; background-image: var(--chev); background-repeat: no-repeat; background-position: right 14px center; }
  .button-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-top: 20px; }
  .button {
    display: inline-flex; align-items: center; justify-content: center; gap: 7px;
    min-height: 40px; padding: 9px 18px; border: 0; border-radius: 999px;
    font-weight: 600; font-size: 14px; white-space: nowrap;
    transition-property: transform, background, color, opacity, box-shadow;
  }
  .button.primary { color: var(--bg); background: var(--ink); }
  .button.primary:hover:not(:disabled) { opacity: .88; }
  .button.secondary { color: var(--ink); background: var(--surface-2); }
  .button.secondary:hover:not(:disabled) { background: var(--line-2); }
  .button.ghost { color: var(--ink-2); background: transparent; box-shadow: inset 0 0 0 1px var(--line-2); }
  .button.ghost:hover:not(:disabled) { color: var(--ink); background: var(--line); }

  /* ── 上传 ── */
  .upload-box {
    position: relative; display: grid; place-items: center; min-height: 168px; padding: 18px;
    border: 1.5px dashed var(--line-2); border-radius: 22px; background: var(--field);
    text-align: center; transition-property: border-color, background;
  }
  .upload-box:hover, .upload-box.dragover { border-color: var(--ink-3); background: var(--surface-2); }
  .upload-box.has-file { border-style: solid; border-color: var(--ink); background: var(--surface); }
  .upload-box input { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
  .upload-icon {
    display: grid; place-items: center; width: 46px; height: 46px; margin: 0 auto 12px; border-radius: 50%;
    background: oklch(var(--cb-l) var(--cb-c) 250); color: oklch(var(--ct-l) var(--ct-c) 250);
  }
  .upload-icon .icon { width: 21px; height: 21px; }
  .upload-box strong { display: block; font-size: 15px; font-weight: 600; overflow-wrap: anywhere; }
  .upload-box span { display: block; margin-top: 3px; color: var(--ink-3); font-size: 13px; }
  .file-meta { min-height: 18px; margin-top: 10px; color: var(--ink-2); font-size: 14px; font-family: var(--num); font-weight: 600; }
  .progress-wrap { display: none; margin-top: 12px; }
  .progress-track { height: 6px; overflow: hidden; border-radius: 3px; background: var(--surface-2); }
  .progress-fill { width: 0; height: 100%; border-radius: inherit; background: var(--ink); transition: width 160ms linear; }
  .progress-label { display: flex; justify-content: space-between; margin-top: 7px; color: var(--ink-2); font-size: 13px; }
  .progress-label #progressPercent { font-family: var(--num); font-weight: 600; color: var(--ink); }

  /* ── 发布前检查 ── */
  .release-checks ul { display: grid; gap: 14px; margin: 0; padding: 0; list-style: none; }
  .release-checks li { display: flex; align-items: center; gap: 11px; color: var(--ink-2); font-size: 14px; }
  .check {
    width: 20px; height: 20px; flex: none; display: grid; place-items: center; border-radius: 50%;
    color: transparent; border: 1.5px dashed var(--ink-3); font-size: 11px; font-weight: 700;
    transition-property: background, color, border-color;
  }
  .check.ok { color: var(--bg); background: var(--ink); border: 1.5px solid var(--ink); }
  .empty-state { color: var(--ink-3); text-align: center; padding: 26px 0; font-size: 14px; }

  /* ── APK 列表 ── */
  .apk-row { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 4px; font-size: 14px; }
  .apk-row + .apk-row::before { content: ""; position: absolute; top: 0; left: 44px; right: 0; border-top: 1px solid var(--line); }
  .apk-name { display: flex; align-items: center; gap: 12px; min-width: 0; font-family: var(--mono); font-size: 13px; overflow-wrap: anywhere; }
  .apk-name .icon {
    width: 32px; height: 32px; padding: 7px; border-radius: 10px;
    background: oklch(var(--cb-l) var(--cb-c) 150); color: oklch(var(--ct-l) var(--ct-c) 150);
  }
  .apk-tag { flex: none; padding: 3px 10px; color: var(--ink-2); background: var(--surface-2); border-radius: 999px; font-size: 12px; }
  .apk-tag.live { color: #fff; background: var(--seal); }

  /* ── 统计 ── */
  .metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
  .metric { padding: 20px 22px; background: var(--surface); border-radius: 24px; }
  .metric-label { color: var(--ink-2); font-size: 13px; font-weight: 550; }
  .metric-value { margin-top: 8px; font-family: var(--num); font-size: 38px; font-weight: 600; line-height: 1; font-variant-numeric: tabular-nums; }
  .metric-note { margin-top: 8px; color: var(--ink-3); font-size: 12px; }
  .stats-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-top: 16px; }
  .stats-grid .card { margin: 0; }
  .consent-item { display: grid; grid-template-columns: 56px 1fr 48px; align-items: center; gap: 12px; margin: 16px 0; font-size: 14px; }
  .consent-item .label { color: var(--ink-2); }
  .mini-track, .bar-track { height: 10px; overflow: hidden; border-radius: 5px; background: var(--surface-2); }
  .mini-fill, .bar-fill { height: 100%; min-width: 4px; border-radius: inherit; transition: width 500ms var(--ease-out); }
  .consent-item b, .bar-count { text-align: right; font-family: var(--num); font-size: 15px; font-weight: 600; font-variant-numeric: tabular-nums; }
  .bar-list { display: grid; gap: 12px; }
  .bar-row { display: grid; grid-template-columns: minmax(90px, 150px) minmax(80px, 1fr) 46px; align-items: center; gap: 12px; font-size: 13px; }
  .bar-name { overflow: hidden; color: var(--ink-2); text-overflow: ellipsis; white-space: nowrap; }
  .trend-chart { display: grid; gap: 8px; }
  .trend-columns { display: grid; grid-template-columns: repeat(30, minmax(0, 1fr)); align-items: end; gap: 5px; height: 160px; border-bottom: 1px solid var(--line-2); }
  .trend-column { height: 100%; display: flex; align-items: end; border-radius: 5px; }
  .trend-bar { display: block; width: 100%; min-height: 2px; background: var(--ink-3); opacity: .55; border-radius: 5px 5px 0 0; transition: opacity 200ms, background 200ms; }
  .trend-column:hover .trend-bar, .trend-column:focus-visible .trend-bar { background: var(--ink); opacity: 1; }
  .trend-column:last-child .trend-bar { background: var(--seal); opacity: 1; }
  .trend-axis { display: flex; justify-content: space-between; color: var(--ink-3); font-family: var(--num); font-size: 13px; font-weight: 500; }
  .chart-footnote { margin-top: 12px; color: var(--ink-2); font-size: 13px; }
  .stats-notice { padding: 12px 16px; margin: 0 0 16px; color: var(--ink-2); background: var(--surface-2); border-radius: 16px; font-size: 13px; }
  .muted { color: var(--ink-2); }
  .notice { display: none; padding: 11px 14px; margin-top: 14px; border-radius: 14px; color: var(--seal); background: var(--seal-soft); font-size: 14px; font-weight: 550; }
  .notice.show { display: block; }

  .toast {
    position: fixed; left: 50%; bottom: 28px; z-index: 10; max-width: min(420px, calc(100vw - 32px));
    padding: 11px 20px; color: var(--bg); background: var(--ink); border-radius: 999px;
    box-shadow: 0 16px 40px -10px rgba(0, 0, 0, .4); font-size: 14px; font-weight: 550;
    opacity: 0; transform: translate(-50%, 10px); pointer-events: none;
    transition: opacity 200ms var(--ease-out), transform 200ms var(--ease-out);
  }
  .toast.show { opacity: 1; transform: translate(-50%, 0); }

  /* ── 动效 ── */
  @keyframes rise { from { opacity: 0; transform: translateY(10px); } }
  @keyframes blink { 50% { opacity: .3; } }
  section[id$="Pane"]:not(.hidden) { animation: rise .35s var(--ease-out); }
  .metrics > *, .stats-grid > *, .publish-grid > * { animation: rise .5s var(--ease-out) both; }
  .metrics > *:nth-child(2), .stats-grid > *:nth-child(2), .publish-grid > *:nth-child(2) { animation-delay: 45ms; }
  .metrics > *:nth-child(3), .stats-grid > *:nth-child(3) { animation-delay: 90ms; }
  .metrics > *:nth-child(4), .stats-grid > *:nth-child(4) { animation-delay: 135ms; }
  .stats-grid > *:nth-child(5) { animation-delay: 180ms; }
  .stats-grid > *:nth-child(6) { animation-delay: 225ms; }
  .stats-grid > *:nth-child(7) { animation-delay: 270ms; }

  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after { animation-duration: .01ms !important; transition-duration: .01ms !important; }
    button:active:not(:disabled) { transform: none; }
  }
  @media (hover: none) {
    .nav button:hover:not(.active) { background: transparent; }
    .upload-box:hover:not(.has-file) { border-color: var(--line-2); background: var(--field); }
  }
  @media (max-width: 900px) {
    .app-shell { grid-template-columns: 1fr; }
    .sidebar { flex-direction: row; align-items: center; gap: 14px; padding: 12px 16px; border-right: 0; border-bottom: 1px solid var(--line); }
    .sidebar-brand { padding: 0; }
    .sidebar-brand span, .sidebar-footer { display: none; }
    .nav { grid-template-columns: 1fr 1fr; flex: 1; }
    .nav button { justify-content: center; }
    .publish-grid, .stats-grid { grid-template-columns: 1fr; }
  }
  @media (max-width: 600px) {
    .sidebar-brand strong { display: none; }
    .content { padding: 22px 14px 48px; }
    .topbar, .page-heading { align-items: flex-start; flex-direction: column; }
    .topbar-actions { width: 100%; justify-content: space-between; }
    .metrics { grid-template-columns: 1fr 1fr; }
    .field-grid { grid-template-columns: 1fr; }
    .card { padding: 20px; border-radius: 24px; }
    .bar-row { grid-template-columns: 90px minmax(50px, 1fr) 38px; gap: 8px; }
    .trend-columns { gap: 3px; }
    .metric-value { font-size: 30px; }
    .cr-ver b { font-size: 38px; }
  }
</style>
</head>
<body>
  <svg width="0" height="0" style="position:absolute" aria-hidden="true">
    <defs>
      <symbol id="seal-mark" viewBox="0 0 40 40">
        <circle cx="20" cy="20" r="20" fill="#C8161D"/>
        <circle cx="20" cy="20" r="15.6" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="1.1"/>
        <text x="20" y="25.4" text-anchor="middle" font-size="15" font-weight="700" fill="#fff" font-family="system-ui, 'PingFang SC', 'Microsoft YaHei', sans-serif">齐</text>
      </symbol>
      <symbol id="seal-wm" viewBox="0 0 200 200">
        <path id="arcT" d="M26 100a74 74 0 0 1 148 0" fill="none"/>
        <path id="arcB" d="M18 100a82 82 0 0 0 164 0" fill="none"/>
        <circle cx="100" cy="100" r="97" fill="none" stroke="currentColor" stroke-width="4"/>
        <circle cx="100" cy="100" r="58" fill="none" stroke="currentColor" stroke-width="2.5"/>
        <text fill="currentColor" font-size="22" font-weight="700" letter-spacing="6"><textPath href="#arcT" startOffset="50%" text-anchor="middle">齐鲁理工学院</textPath></text>
        <text fill="currentColor" font-size="13" font-weight="700" letter-spacing="1.5" font-family="system-ui"><textPath href="#arcB" startOffset="50%" text-anchor="middle" dominant-baseline="hanging">QILU INSTITUTE OF TECHNOLOGY</textPath></text>
      </symbol>
    </defs>
  </svg>

  <section id="loginScreen" class="auth-shell">
    <button class="icon-btn" data-theme-toggle onclick="toggleTheme()" aria-label="切换深色模式"></button>
    <div class="auth-card">
      <svg class="brand-mark" aria-hidden="true"><use href="#seal-mark"/></svg>
      <h1>QLIT 发布中心</h1>
      <p>发布新版本、管理安装包、查看匿名使用统计。</p>
      <div class="field">
        <label for="token">管理口令</label>
        <input id="token" type="password" autocomplete="current-password" placeholder="输入 ADMIN_TOKEN" onkeydown="if(event.key === 'Enter') login()">
      </div>
      <button id="loginButton" class="button primary" style="width:100%; margin-top:20px; min-height:46px" onclick="login()">进入后台</button>
      <div id="loginError" class="notice" role="alert"></div>
    </div>
  </section>

  <div id="appShell" class="app-shell hidden">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <svg class="brand-mark" aria-hidden="true"><use href="#seal-mark"/></svg>
        <div><strong>QLIT 后台</strong><span>发布与设备概览</span></div>
      </div>
      <nav class="nav" aria-label="后台导航">
        <button class="active" data-tab="update" onclick="switchTab('update')"><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 7.5 12 3.5l8.5 4v9L12 20.5l-8.5-4z"/><path d="M3.5 7.5 12 11.5l8.5-4M12 11.5v9"/></svg>版本发布</button>
        <button data-tab="stats" onclick="switchTab('stats')"><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20.5h16"/><rect x="5" y="11" width="3.2" height="6.5" rx="1.2"/><rect x="10.4" y="6" width="3.2" height="11.5" rx="1.2"/><rect x="15.8" y="9" width="3.2" height="8.5" rx="1.2"/></svg>使用统计</button>
      </nav>
      <div class="sidebar-footer"><strong>QLIT 小助手</strong><span>齐鲁理工学院校园助手</span></div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div><h1 id="pageTitle">发布中心</h1><p id="lastUpdated">正在读取服务状态…</p></div>
        <div class="topbar-actions">
          <span id="serviceStatus" class="status-chip" data-state="loading"><span class="status-dot"></span><span id="serviceStatusText">连接中</span></span>
          <span style="display:flex;gap:4px">
            <button class="icon-btn" data-theme-toggle onclick="toggleTheme()" aria-label="切换深色模式"></button>
            <button id="logoutButton" class="button ghost" onclick="logout()"><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9.5 20.5H6A1.5 1.5 0 0 1 4.5 19V5A1.5 1.5 0 0 1 6 3.5h3.5"/><path d="m15.5 16.5 4.5-4.5-4.5-4.5M20 12H9.5"/></svg>退出</button>
          </span>
        </div>
      </header>

      <section id="updatePane">
        <div class="current-release" id="currentReleaseCard">
          <svg class="seal-wm" aria-hidden="true"><use href="#seal-wm"/></svg>
          <div id="currentRelease"></div>
        </div>
        <details id="releaseNotes" class="release-notes hidden"><summary>线上更新日志</summary><pre id="currentChangelog"></pre></details>

        <div class="page-heading"><div><h2>发布新版本</h2><p>填好版本信息和更新日志，选好安装包，一次完成发布。</p></div></div>
        <fieldset id="publishForm">
        <div class="publish-grid">
          <div class="card">
            <div class="card-title"><div><h3>版本信息</h3><p>版本号必须高于当前线上版本。</p></div></div>
            <div class="field-grid">
              <div class="field"><label for="versionName">版本名</label><input id="versionName" type="text" placeholder="例如 2.1.0" oninput="updateChecklist()"></div>
              <div class="field"><label for="versionCode">版本号</label><input id="versionCode" type="number" min="1" placeholder="例如 10" oninput="updateChecklist()"></div>
            </div>
            <div class="field" style="margin-top:14px"><label for="changelog">更新日志</label><textarea id="changelog" placeholder="每行写一项更新内容，例如：
修复出行登记在部分设备上无法获取 Cookie 的问题
优化课表首页加载速度" oninput="updateChecklist()"></textarea></div>
          </div>

          <div class="card release-checks">
            <div class="card-title"><div><h3>发布前检查</h3><p>三项都打勾再发布。</p></div></div>
            <ul>
              <li><span id="checkVersion" class="check">•</span><span id="versionCheckText">版本名和版本号已填写</span></li>
              <li><span id="checkLog" class="check">•</span><span>更新日志已填写</span></li>
              <li><span id="checkApk" class="check">•</span><span>已选择或上传 APK</span></li>
            </ul>
            <div class="field" style="margin-top:24px"><label for="existingApk">使用已上传的安装包</label><select id="existingApk" onchange="onExistingApkChange()"><option value="">选择现有 APK（可选）</option></select></div>
          </div>
        </div>

        <div class="card" style="margin-top:16px">
          <div class="card-title"><div><h3>安装包</h3><p>选择本地 APK，或在右上方选一个已上传的安装包。最大 300 MB。</p></div></div>
          <div id="uploadBox" class="upload-box">
            <input id="apk" type="file" aria-label="选择安装包" accept=".apk,application/vnd.android.package-archive" onchange="onFileChange()">
            <div><div class="upload-icon"><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 15.5V4M7.5 8.5 12 4l4.5 4.5M5 19.5h14"/></svg></div><strong id="uploadTitle">点击选择或拖入 APK 文件</strong><span id="uploadHint">文件名支持英文字母、数字、点、下划线和短横线</span></div>
          </div>
          <div id="fileMeta" class="file-meta"></div>
          <div id="progressWrap" class="progress-wrap"><div class="progress-track"><div id="progressFill" class="progress-fill"></div></div><div class="progress-label"><span id="progressText">准备上传</span><span id="progressPercent">0%</span></div></div>
          <div id="publishError" class="notice" role="alert"></div>
          <div class="button-row"><button id="publishButton" class="button primary" onclick="publish()">上传并发布</button><button class="button ghost" onclick="resetForm()">清空表单</button><span id="publishHint" class="muted">发布后，App 检查更新时会收到这个版本。</span></div>
        </div>
        </fieldset>

        <div class="card" style="margin-top:16px">
          <div class="card-title"><div><h3>已上传文件</h3><p>选择已有安装包时，无需再次上传。</p></div><button id="stateRefresh" class="button ghost" onclick="refreshState()">刷新</button></div>
          <div id="apkList" class="muted">正在读取…</div>
        </div>
      </section>

      <section id="statsPane" class="hidden">
        <div class="page-heading"><div><h2>设备与上报</h2><p>设备分布和每天的上报趋势。</p></div><div class="button-row" style="margin-top:0"><button id="csvButton" class="button ghost" onclick="downloadCsv()">导出 CSV</button><button id="statsRefresh" class="button secondary" onclick="refreshStats()">刷新统计</button></div></div>
        <div class="stats-notice">统计来自当前保留的上报记录，不是实时在线人数。日期按 UTC 计算；App 版本占比按上报次数计算。</div>
        <div id="statsError" class="notice" role="alert" style="margin:0 0 16px"></div>
        <div class="metrics">
          <div class="metric"><div class="metric-label">设备总数</div><div id="metricDevices" class="metric-value">—</div><div class="metric-note">去重后的设备</div></div>
          <div class="metric"><div class="metric-label">保留记录数</div><div id="metricPings" class="metric-value">—</div><div class="metric-note">当前文件与保留的归档</div></div>
          <div class="metric"><div class="metric-label">同意率</div><div id="metricConsent" class="metric-value">—</div><div class="metric-note">只计算已选择的设备</div></div>
          <div class="metric"><div class="metric-label">最近上报</div><div id="metricLatest" class="metric-value" style="font-size:26px">—</div><div class="metric-note">按自然日统计</div></div>
        </div>
        <div class="stats-grid">
          <div class="card"><div class="card-title"><div><h3>统计同意情况</h3><p>每台设备最后一次的选择。</p></div></div><div id="consentChart"></div></div>
          <div class="card"><div class="card-title"><div><h3>App 版本</h3><p>按上报次数排序 · 前 12 项</p></div></div><div id="appVersionChart" class="bar-list"></div></div>
          <div class="card"><div class="card-title"><div><h3>设备品牌</h3><p>品牌分布 · 前 12 项</p></div></div><div id="brandChart" class="bar-list"></div></div>
          <div class="card"><div class="card-title"><div><h3>Android / API</h3><p>系统版本和 API 级别 · 前 12 项</p></div></div><div id="osChart" class="bar-list"></div></div>
          <div class="card"><div class="card-title"><div><h3>ROM 版本</h3><p>系统差异 · 前 12 项</p></div></div><div id="romChart" class="bar-list"></div></div>
          <div class="card"><div class="card-title"><div><h3>机型</h3><p>设备型号排行 · 前 12 项</p></div></div><div id="modelChart" class="bar-list"></div></div>
          <div class="card" style="grid-column:1/-1"><div class="card-title"><div><h3>每日上报量</h3><p>最近 30 个自然日 · UTC · 红色是最近一天</p></div></div><div id="dailyChart" class="trend-chart"></div><div id="dailySummary" class="chart-footnote"></div></div>
        </div>
      </section>
    </main>
  </div>
  <div id="toast" class="toast" role="status"></div>

<script>
  const $ = (id) => document.getElementById(id);
  let TOKEN = localStorage.getItem('qlit_admin_token') || '';
  let currentApks = [];
  let liveApk = '';
  let toastTimer = null;

  const ICON_PACKAGE = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 7.5 12 3.5l8.5 4v9L12 20.5l-8.5-4z"/><path d="M3.5 7.5 12 11.5l8.5-4M12 11.5v9"/></svg>';
  const ICON_SUN = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/></svg>';
  const ICON_MOON = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"><path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/></svg>';
  // 与 App 课程色盘同一套 OKLCH 色相，避开校徽红所在的 15–35°
  const HUE = { version: 250, brand: 150, os: 285, rom: 185, model: 70, agreed: 150, rejected: 70 };
  const tint = (h) => 'oklch(var(--cr-l) var(--cr-c) ' + h + ')';

  function paintThemeButtons() {
    const dark = document.documentElement.dataset.theme === 'dark';
    document.querySelectorAll('[data-theme-toggle]').forEach((b) => {
      b.innerHTML = dark ? ICON_SUN : ICON_MOON;
      b.setAttribute('aria-label', dark ? '切换浅色模式' : '切换深色模式');
    });
  }
  function toggleTheme() {
    const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem('qlit_admin_theme', next); } catch (e) {}
    paintThemeButtons();
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function showToast(message) {
    const node = $('toast');
    node.textContent = message;
    node.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => node.classList.remove('show'), 2600);
  }

  function showNotice(id, message) {
    const node = $(id);
    node.textContent = message || '';
    node.classList.toggle('show', Boolean(message));
  }

  function setServiceStatus(state, text) {
    const chip = $('serviceStatus');
    chip.dataset.state = state;
    $('serviceStatusText').textContent = text;
  }

  async function api(path, options) {
    const opts = options || {};
    const headers = Object.assign({}, opts.headers || {}, { 'X-Admin-Token': TOKEN });
    const res = await fetch(path, Object.assign({}, opts, { headers }));
    if (res.status === 403) {
      logout();
      throw new Error('管理口令已失效');
    }
    return res;
  }

  async function login() {
    const input = $('token');
    const button = $('loginButton');
    TOKEN = input.value.trim();
    showNotice('loginError', '');
    if (!TOKEN) { showNotice('loginError', '请输入管理口令'); return; }
    button.disabled = true;
    button.textContent = '验证中…';
    try {
      const res = await fetch('/admin/state', { headers: { 'X-Admin-Token': TOKEN } });
      if (res.status !== 200) throw new Error('口令错误');
      localStorage.setItem('qlit_admin_token', TOKEN);
      $('loginScreen').classList.add('hidden');
      $('appShell').classList.remove('hidden');
      await refreshState();
      await refreshStats();
      setServiceStatus('ok', '已连接');
    } catch (error) {
      showNotice('loginError', error.message || '无法连接服务');
    } finally {
      button.disabled = false;
      button.textContent = '进入后台';
    }
  }

  function logout() {
    localStorage.removeItem('qlit_admin_token');
    TOKEN = '';
    $('appShell').classList.add('hidden');
    $('loginScreen').classList.remove('hidden');
    $('token').value = '';
  }

  function switchTab(tab) {
    document.querySelectorAll('[data-tab]').forEach((button) => button.classList.toggle('active', button.dataset.tab === tab));
    $('updatePane').classList.toggle('hidden', tab !== 'update');
    $('statsPane').classList.toggle('hidden', tab !== 'stats');
    $('pageTitle').textContent = tab === 'stats' ? '使用统计' : '发布中心';
    if (tab === 'stats') refreshStats();
  }

  function formatBytes(bytes) {
    if (!Number.isFinite(bytes) || bytes <= 0) return '—';
    const units = ['B', 'KB', 'MB', 'GB'];
    const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return (bytes / Math.pow(1024, index)).toFixed(index ? 1 : 0) + ' ' + units[index];
  }

  function formatTime(value) {
    const date = value ? new Date(value) : null;
    if (!date || isNaN(date)) return '未知';
    return date.toLocaleString('zh-CN', { hour12: false });
  }

  function renderCurrentRelease(state) {
    const card = $('currentReleaseCard');
    card.classList.toggle('is-empty', !state.versionName);
    $('currentRelease').innerHTML = state.versionName
      ? '<div class="cr-top"><span>线上版本</span><span class="num">' + escapeHtml(formatTime(state.publishedAt)) + '</span></div>'
        + '<div class="cr-ver"><b>' + escapeHtml(state.versionName) + '</b><small>#' + escapeHtml(state.versionCode) + '</small></div>'
        + '<div class="cr-meta">安装包 <span class="num">' + escapeHtml(state.apkFile || '未知') + '</span></div>'
      : '<div class="cr-top"><span>线上版本</span></div>'
        + '<div class="cr-ver is-empty"><b>还没有发布过版本</b></div>'
        + '<div class="cr-meta">发布后，这里会显示 App 当前能收到的版本。</div>';
  }

  async function refreshState() {
    const button = $('stateRefresh');
    if (button) { button.disabled = true; button.textContent = '读取中…'; }
    try {
      const res = await api('/admin/state');
      const data = await res.json();
      const state = data.update || {};
      currentApks = Array.isArray(data.apks) ? data.apks.slice().sort() : [];
      liveApk = state.apkFile || '';
      $('lastUpdated').textContent = '状态已更新 · ' + new Date().toLocaleString('zh-CN', { hour12: false });
      renderCurrentRelease(state);
      const notes = $('releaseNotes');
      notes.classList.toggle('hidden', !state.changelog);
      $('currentChangelog').textContent = state.changelog || '';
      if (state.versionCode) {
        $('versionCheckText').textContent = '版本号高于当前 #' + state.versionCode;
        $('versionCode').min = String(Number(state.versionCode) + 1);
      } else {
        $('versionCheckText').textContent = '版本名和版本号已填写';
        $('versionCode').min = '1';
      }
      const select = $('existingApk');
      select.innerHTML = '<option value="">选择现有 APK（可选）</option>';
      currentApks.forEach((name) => {
        const option = document.createElement('option');
        option.value = name;
        option.textContent = name;
        select.appendChild(option);
      });
      $('apkList').innerHTML = currentApks.length
        ? currentApks.map((name) => '<div class="apk-row"><span class="apk-name">' + ICON_PACKAGE + escapeHtml(name) + '</span>'
            + (name === liveApk ? '<span class="apk-tag live">线上</span>' : '<span class="apk-tag">已上传</span>') + '</div>').join('')
        : '<div class="empty-state">还没有上传 APK</div>';
      updateChecklist();
      setServiceStatus('ok', '已连接');
    } catch (error) {
      setServiceStatus('error', '连接失败');
      showToast(error.message || '读取版本状态失败');
    } finally {
      if (button) { button.disabled = false; button.textContent = '刷新'; }
    }
  }

  const UPLOAD_TITLE = '点击选择或拖入 APK 文件';
  const UPLOAD_HINT = '文件名支持英文字母、数字、点、下划线和短横线';

  function onFileChange() {
    const file = $('apk').files[0];
    const box = $('uploadBox');
    box.classList.toggle('has-file', Boolean(file));
    $('uploadTitle').textContent = file ? file.name : UPLOAD_TITLE;
    $('uploadHint').textContent = file ? '已选择文件，可以开始发布' : UPLOAD_HINT;
    $('fileMeta').textContent = file ? formatBytes(file.size) : '';
    if (file) $('existingApk').value = '';
    updateChecklist();
  }

  function onExistingApkChange() {
    if ($('existingApk').value) {
      $('apk').value = '';
      $('uploadBox').classList.remove('has-file');
      $('uploadTitle').textContent = UPLOAD_TITLE;
      $('uploadHint').textContent = '已选择已有安装包，如需替换请重新选择文件';
      $('fileMeta').textContent = '';
    }
    updateChecklist();
  }

  function updateChecklist() {
    const versionOk = Boolean($('versionName').value.trim() && parseInt($('versionCode').value, 10) >= parseInt($('versionCode').min || '1', 10));
    const logOk = Boolean($('changelog').value.trim());
    const apkOk = Boolean($('apk').files[0] || $('existingApk').value);
    [['checkVersion', versionOk], ['checkLog', logOk], ['checkApk', apkOk]].forEach(([id, ok]) => {
      const node = $(id);
      node.classList.toggle('ok', ok);
      node.textContent = ok ? '✓' : '•';
    });
  }

  function uploadApk(file) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', '/admin/apk?name=' + encodeURIComponent(file.name));
      xhr.setRequestHeader('X-Admin-Token', TOKEN);
      xhr.upload.onprogress = (event) => {
        if (!event.lengthComputable) return;
        const percent = Math.round(event.loaded / event.total * 100);
        $('progressWrap').style.display = 'block';
        $('progressFill').style.width = percent + '%';
        $('progressText').textContent = '安装包上传中';
        $('progressPercent').textContent = percent + '%';
      };
      xhr.onload = () => xhr.status === 204 ? resolve() : reject(new Error('上传失败（HTTP ' + xhr.status + '）'));
      xhr.onerror = () => reject(new Error('网络连接失败'));
      xhr.onabort = () => reject(new Error('上传已取消'));
      xhr.send(file);
    });
  }

  async function publish() {
    showNotice('publishError', '');
    const versionName = $('versionName').value.trim();
    const versionCode = parseInt($('versionCode').value, 10);
    const changelog = $('changelog').value.trim();
    const file = $('apk').files[0];
    const selectedApk = $('existingApk').value;
    if (!versionName || !versionCode || versionCode < 1) { showNotice('publishError', '请填写有效的版本名和版本号'); return; }
    if (!changelog) { showNotice('publishError', '请填写更新日志'); return; }
    if (!file && !selectedApk) { showNotice('publishError', '请选择 APK，或选择一个已上传的安装包'); return; }
    const button = $('publishButton');
    button.disabled = true;
    button.textContent = '发布中…';
    try {
      let apkFile = selectedApk;
      if (file) {
        if (!file.name.toLowerCase().endsWith('.apk')) throw new Error('请选择 .apk 文件');
        await uploadApk(file);
        apkFile = file.name;
      }
      const res = await api('/admin/publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ versionName, versionCode, changelog, apkFile }),
      });
      const result = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(result.error || '发布失败（HTTP ' + res.status + '）');
      showToast('已发布 v' + versionName);
      $('progressWrap').style.display = 'none';
      resetForm();
      await refreshState();
    } catch (error) {
      showNotice('publishError', error.message || '发布失败');
    } finally {
      button.disabled = false;
      button.textContent = '上传并发布';
    }
  }

  function resetForm() {
    $('versionName').value = '';
    $('versionCode').value = '';
    $('changelog').value = '';
    $('apk').value = '';
    $('existingApk').value = '';
    $('uploadBox').classList.remove('has-file');
    $('uploadTitle').textContent = UPLOAD_TITLE;
    $('uploadHint').textContent = UPLOAD_HINT;
    $('fileMeta').textContent = '';
    $('progressWrap').style.display = 'none';
    showNotice('publishError', '');
    updateChecklist();
  }

  function animateNumber(el, target, suffix) {
    const to = Number(target) || 0;
    const start = performance.now();
    const duration = 650;
    function tick(now) {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = Math.round(to * eased) + (suffix || '');
      if (t < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  function renderBars(id, list, hue, limit) {
    const node = $(id);
    node.textContent = '';
    const rows = (list || []).slice(0, limit || 12);
    if (!rows.length) { node.innerHTML = '<div class="empty-state">暂无数据</div>'; return; }
    const max = Math.max(1, rows[0][1]);
    rows.forEach(([name, count]) => {
      const row = document.createElement('div');
      row.className = 'bar-row';
      const label = document.createElement('div');
      label.className = 'bar-name'; label.textContent = name; label.title = name;
      const track = document.createElement('div'); track.className = 'bar-track';
      const fill = document.createElement('div'); fill.className = 'bar-fill'; fill.style.width = Math.max(3, count / max * 100) + '%'; fill.style.background = tint(hue);
      const value = document.createElement('div'); value.className = 'bar-count'; value.textContent = count;
      track.appendChild(fill); row.append(label, track, value); node.appendChild(row);
    });
  }

  function renderConsent(consent) {
    const total = consent.agreed + consent.rejected + consent.undecided;
    const answered = consent.agreed + consent.rejected;
    const rate = answered ? Math.round(consent.agreed / answered * 100) : 0;
    animateNumber($('metricConsent'), rate, '%');
    const items = [['同意', consent.agreed, tint(HUE.agreed)], ['拒绝', consent.rejected, tint(HUE.rejected)], ['未选择', consent.undecided, 'var(--ink-3)']];
    $('consentChart').innerHTML = items.map(([label, count, color]) => {
      const percent = total ? Math.round(count / total * 100) : 0;
      return '<div class="consent-item"><span class="label">' + label + '</span><div class="mini-track"><div class="mini-fill" style="width:' + percent + '%;background:' + color + '"></div></div><b>' + count + '</b></div>';
    }).join('') + '<div class="muted" style="margin-top:18px;font-size:13px">共 ' + total + ' 台设备，已选择 ' + answered + ' 台</div>';
  }

  async function refreshStats() {
    const button = $('statsRefresh');
    if (button) { button.disabled = true; button.textContent = '读取中…'; }
    showNotice('statsError', '');
    try {
      const res = await api('/stats.json');
      const stats = await res.json();
      const days = Object.keys(stats.dailyPings || {}).sort();
      animateNumber($('metricDevices'), stats.totalDevices || 0);
      animateNumber($('metricPings'), stats.totalPings || 0);
      $('metricLatest').textContent = days[days.length - 1] || '—';
      renderConsent(stats.consent || { agreed: 0, rejected: 0, undecided: 0 });
      renderBars('appVersionChart', stats.pingsByAppVersion, HUE.version);
      renderBars('brandChart', stats.devicesByBrand, HUE.brand);
      renderBars('osChart', stats.devicesByOs, HUE.os);
      renderBars('romChart', stats.devicesByRom, HUE.rom);
      renderBars('modelChart', stats.devicesByModel, HUE.model);
      renderDaily(days.slice(-30).map((day) => [day, stats.dailyPings[day]]));
      setServiceStatus('ok', '已连接');
    } catch (error) {
      setServiceStatus('error', '连接失败');
      showNotice('statsError', error.message || '读取统计失败');
      showToast(error.message || '读取统计失败');
    } finally {
      if (button) { button.disabled = false; button.textContent = '刷新统计'; }
    }
  }

  function renderDaily(rows) {
    const node = $('dailyChart');
    const summary = $('dailySummary');
    if (!rows.length) {
      node.innerHTML = '<div class="empty-state">暂无数据</div>';
      summary.textContent = '';
      return;
    }
    const max = Math.max(1, ...rows.map((row) => row[1]));
    node.innerHTML = '<div class="trend-columns" aria-label="每日上报量">' + rows.map(([day, count]) => {
      const height = Math.max(2, Math.round(count / max * 100));
      return '<div class="trend-column" tabindex="0" title="' + escapeHtml(day) + '：' + count + '"><span class="trend-bar" style="height:' + height + '%"></span></div>';
    }).join('') + '</div><div class="trend-axis"><span>' + escapeHtml(rows[0][0]) + '</span><span>' + escapeHtml(rows[rows.length - 1][0]) + '</span></div>';
    const total = rows.reduce((sum, row) => sum + Number(row[1] || 0), 0);
    const defaultSummary = '最近 ' + rows.length + ' 天共上报 ' + total + ' 条，峰值 ' + max + ' 条。';
    summary.textContent = defaultSummary;
    node.querySelectorAll('.trend-column').forEach((col, index) => {
      const [day, count] = rows[index];
      const show = () => { summary.textContent = day + ' · ' + count + ' 条上报'; };
      const hide = () => { summary.textContent = defaultSummary; };
      col.addEventListener('mouseenter', show);
      col.addEventListener('focus', show);
      col.addEventListener('mouseleave', hide);
      col.addEventListener('blur', hide);
    });
  }

  // ── 设备签发 ──

  function copyText(id) {
    const text = $(id).textContent;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(
        () => showToast('已复制'),
        () => showToast('复制失败'),
      );
      return;
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); showToast('已复制'); } catch (e) { showToast('复制失败'); }
    ta.remove();
  }

  async function downloadCsv() {
    try {
      const res = await api('/admin/pings.csv');
      if (!res.ok) throw new Error('导出失败');
      const blob = await res.blob();
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob); link.download = 'pings.csv'; link.click();
      URL.revokeObjectURL(link.href);
      showToast('CSV 已开始下载');
    } catch (error) { showToast(error.message || '导出失败'); }
  }

  ['dragenter', 'dragover'].forEach((evt) => $('uploadBox').addEventListener(evt, () => $('uploadBox').classList.add('dragover')));
  ['dragleave', 'drop'].forEach((evt) => $('uploadBox').addEventListener(evt, () => $('uploadBox').classList.remove('dragover')));

  paintThemeButtons();
  renderCurrentRelease({});
  if (TOKEN) {
    $('loginScreen').classList.add('hidden');
    $('appShell').classList.remove('hidden');
    refreshState(); refreshStats();
  }
  updateChecklist();
</script>
</body>
</html>`;
}

module.exports = { adminPageHtml };
