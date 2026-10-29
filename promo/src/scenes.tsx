import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {BOUNCY, C, CJK, EASE, NUM, SLOW, SOFT, SNAPPY, glyph, lerp, ramp, spr, tint, useT} from './theme';
import {AppIcon, Backdrop, BigType, Chip, Headline, Icon, LiftOut, Phone, RATIO} from './ui';

const SHADOW = '0 26px 50px -18px rgba(40,25,10,.38), 0 3px 8px rgba(40,25,10,.08)';
const float = (f: number, amp = 8, period = 90, phase = 0) => Math.sin(((f + phase) / period) * Math.PI * 2) * amp;
const screenTop = (cy: number, w: number) => cy - (w * RATIO) / 2;

/** Centered line whose characters rise out of a mask one by one. */
const RiseLine: React.FC<{parts: {t: string; font?: string; color: string; weight?: number}[]; size: number; delay: number; top: number; step?: number; dur?: number}> = ({parts, size, delay, top, step = 2, dur = 22}) => {
  const f = useT();
  let i = 0;
  return (
    <div style={{position: 'absolute', left: 0, right: 0, top, textAlign: 'center', whiteSpace: 'nowrap', overflow: 'hidden', lineHeight: 1.18, paddingBottom: 8}}>
      {parts.map((part, pi) => (
        <span key={pi} style={{fontFamily: part.font ?? CJK, fontWeight: part.weight ?? 850, fontSize: size, color: part.color}}>
          {[...part.t].map((ch) => {
            const t0 = delay + i++ * step;
            const p = ramp(f, t0, t0 + dur);
            return (
              <span key={i} style={{display: 'inline-block', whiteSpace: 'pre', transform: `translateY(${(1 - p) * 115}%)`}}>{ch}</span>
            );
          })}
        </span>
      ))}
    </div>
  );
};

/* ---------- 1. Seal stamp ---------- */
export const Intro: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const drop = spr(f, fps, 4, {damping: 14, stiffness: 240, mass: 0.9});
  const hit = 12;
  const since = f - hit;
  const shake = since > 0 ? 16 * Math.exp(-since / 6) * Math.sin(since * 2.4) : 0;
  const ring = (d: number) => {
    const p = ramp(f, hit + d, hit + d + 36);
    return p > 0 && p < 1 ? (
      <div style={{position: 'absolute', left: 540, top: 760, width: 560, height: 560, marginLeft: -280, marginTop: -280, borderRadius: '50%',
        border: `${6 * (1 - p) + 1}px solid ${C.seal}`, transform: `scale(${0.9 + p * 1.5})`, opacity: (1 - p) * 0.6}} />
    ) : null;
  };
  const out = ramp(f, 100, 160, 0, 1);
  return (
    <Backdrop color={C.bg}>
      <AbsoluteFill style={{transform: `translate(${shake * 0.6}px, ${shake}px) scale(${1 + out * 0.05})`}}>
        <div style={{position: 'absolute', left: 540, top: 760, width: 900, height: 900, marginLeft: -450, marginTop: -450, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(200,22,29,.14), transparent 62%)', opacity: ramp(f, hit, hit + 10)}} />
        {ring(0)}
        {ring(7)}
        <div style={{position: 'absolute', left: 540, top: 760, width: 520, height: 520, marginLeft: -260, marginTop: -260,
          transform: `scale(${interpolate(drop, [0, 1], [3.2, 1])}) rotate(${interpolate(drop, [0, 1], [-28, 0])}deg)`, opacity: ramp(f, 4, 9)}}>
          <AppIcon size={520} shape="seal" style={{boxShadow: `0 ${40 * drop}px ${80 * drop}px -30px rgba(120,20,10,.35)`}} />
        </div>
        <RiseLine top={1170} size={150} delay={28} parts={[{t: 'QLIT', font: NUM, color: C.ink, weight: 700}, {t: ' 小助手', color: C.ink}]} />
        <RiseLine top={1380} size={46} delay={44} step={1.2} dur={18} parts={[{t: '齐鲁理工学院的课表与校园工具', color: C.ink2, weight: 550}]} />
      </AbsoluteFill>
    </Backdrop>
  );
};

/* ---------- 2. Kinetic word cuts ---------- */
const BEATS = [
  {w: '上课', en: 'CLASS', icon: 'week', bg: C.seal, fg: '#fff'},
  {w: '取件', en: 'PICKUP', icon: 'pickup', bg: C.ink, fg: '#fff'},
  {w: '浴室', en: 'SHOWER', icon: 'shower', bg: C.bg, fg: C.ink},
  {w: '电费', en: 'POWER', icon: 'bolt', bg: C.seal, fg: '#fff'},
  {w: '作业', en: 'TODO', icon: 'todo', bg: C.ink, fg: '#fff'},
];
// Real frames, not slowed: each word lands on a beat of the 120 BPM track (30 frames).
// LEAD is half of the incoming transition, so the first word hits on the cut.
const BEAT = 30;
const LEAD = 12;
export const Kinetic: React.FC = () => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  const i = Math.max(0, Math.floor((f - LEAD) / BEAT));
  if (i < BEATS.length) {
    const b = BEATS[i];
    const t = Math.max(0, f - LEAD - i * BEAT);
    const s = interpolate(t, [0, 18], [1.24, 1], {extrapolateRight: 'clamp', easing: EASE});
    const ip = spr(t, fps, 0, BOUNCY);
    const faint = b.fg === '#fff' ? 'rgba(255,255,255,.2)' : 'rgba(29,27,24,.12)';
    return (
      <Backdrop color={b.bg} glow={b.bg === C.bg ? 'rgba(255,255,255,.55)' : 'rgba(255,255,255,.08)'}>
        <BigType text={`${b.en} ${b.en}`} top={1270} size={360} stroke={faint} drift={-7} from={40 - i * 120} />
        <BigType text={`${b.en} ${b.en}`} top={160} size={360} stroke={faint} drift={6} from={-1400 + i * 120} />
        <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 10}}>
          <div style={{transform: `scale(${ip}) rotate(${(1 - ip) * -30}deg)`}}>
            <Icon k={b.icon} size={150} color={b.bg === C.bg ? C.seal : b.fg} stroke={1.7} />
          </div>
          <div style={{fontFamily: CJK, fontSize: 330, fontWeight: 900, color: b.fg, lineHeight: 1.1, transform: `scale(${s})`,
            letterSpacing: interpolate(t, [0, 18], [60, 6], {extrapolateRight: 'clamp', easing: EASE})}}>{b.w}</div>
        </AbsoluteFill>
        <div style={{position: 'absolute', left: 84, top: 110, fontFamily: NUM, fontWeight: 600, fontSize: 44, color: b.fg, opacity: 0.7}}>
          {`0${i + 1}`} <span style={{opacity: 0.5}}>/ 0{BEATS.length}</span>
        </div>
      </Backdrop>
    );
  }
  const t = f - LEAD - BEATS.length * BEAT;
  return (
    <Backdrop color={C.bg}>
      <RiseLine top={640} size={120} delay={(LEAD + BEATS.length * BEAT) / SLOW} step={2} dur={16} parts={[{t: '全都放进', color: C.ink}]} />
      <div style={{position: 'absolute', left: 0, right: 0, top: 800, display: 'flex', justifyContent: 'center', gap: 10}}>
        {[...'一页'].map((ch, k) => {
          const p = spr(t, fps, 6 + k * 4, BOUNCY);
          return (
            <span key={k} style={{fontFamily: CJK, fontWeight: 900, fontSize: 340, color: C.seal, lineHeight: 1.1, display: 'inline-block',
              transform: `scale(${p}) rotate(${(1 - p) * (k ? 14 : -14)}deg)`}}>{ch}</span>
          );
        })}
      </div>
      <div style={{position: 'absolute', left: 0, right: 0, top: 1250, display: 'flex', justifyContent: 'center', gap: 34}}>
        {BEATS.map((b, k) => {
          const p = spr(t, fps, 14 + k * 3, BOUNCY);
          const hue = [25, 50, 220, 95, 250][k];
          return (
            <div key={k} style={{width: 112, height: 112, borderRadius: 34, background: tint(hue), display: 'grid', placeItems: 'center', transform: `scale(${p}) translateY(${(1 - p) * 40}px)`}}>
              <Icon k={b.icon} size={60} color={glyph(hue)} />
            </div>
          );
        })}
      </div>
    </Backdrop>
  );
};

/* ---------- 3. Today ---------- */
export const Today: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const e = spr(f, fps, 2, SOFT);
  const lp = spr(f, fps, 66, {damping: 16, stiffness: 170, mass: 0.8});
  const w = 580;
  const cy = 1310 + (1 - e) * 1100 + float(f, 6, 120);
  const flat = ramp(f, 16, 62);
  return (
    <Backdrop color={C.page}>
      <BigType text="10:42 NEXT 10:42" top={1060} size={380} stroke="rgba(29,27,24,.06)" drift={-3} from={0} />
      <Headline kicker="01" label="今天" icon="home" hue={25} lines={[['下一节课，'], ['抬手就', {t: '知道', a: true}]]} />
      <Phone src="proto/light-home.png" cx={540} cy={cy} w={w} rx={(1 - flat) * 16} rotate={(1 - flat) * -5} dim={lp * 0.35} scale={1 - lp * 0.04} />
      <LiftOut src="proto/light-home.png" rect={{x: 16, y: 120, w: 358, h: 237, r: 28}} phone={{cx: 540, cy, w}}
        to={{cx: 540, cy: 1040, scale: 1.42, rotate: -2.5}} p={lp} shine={ramp(f, 86, 126)} />
      <Chip icon="clock" text="倒计时放大" p={spr(f, fps, 100, BOUNCY)} style={{left: 70, top: 1300}} origin="0% 50%" />
      <Chip icon="pin" text="教室号醒目" p={spr(f, fps, 108, BOUNCY)} style={{right: 70, top: 1420}} origin="100% 50%" solid />
    </Backdrop>
  );
};

/* ---------- 4. Week ---------- */
export const Week: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const e = spr(f, fps, 0, SOFT);
  const w = 580;
  const cy = 1310 + float(f, 6, 120, 30);
  const l1 = spr(f, fps, 58, SNAPPY);
  const l2 = spr(f, fps, 68, SNAPPY);
  return (
    <Backdrop color={C.page}>
      <BigType text="WEEK 07 WEEK 07" top={1080} size={380} stroke="rgba(29,27,24,.06)" drift={-3.2} from={-200} />
      <Headline kicker="02" label="课表" icon="week" hue={250} lines={[['课程块直接'], ['铺在', {t: '纸上', a: true}]]} />
      <Phone src="proto/light-week.png" cx={540} cy={cy} w={w} tx={(1 - e) * 1000} ry={(1 - e) * -34} rotate={(1 - e) * 8}
        dim={Math.max(l1, l2) * 0.3} />
      <LiftOut src="proto/light-week.png" rect={{x: 31, y: 195, w: 66, h: 96, r: 10}} phone={{cx: 540, cy, w}}
        to={{cx: 250, cy: 1060, scale: 2.7, rotate: -7}} p={l1} shine={ramp(f, 72, 104)} />
      <LiftOut src="proto/light-week.png" rect={{x: 244, y: 195, w: 66, h: 96, r: 10}} phone={{cx: 540, cy, w}}
        to={{cx: 820, cy: 1200, scale: 2.7, rotate: 6}} p={l2} shine={ramp(f, 80, 112)} />
      <Chip text="同一门课，同一个颜色" p={spr(f, fps, 88, BOUNCY)} style={{left: 0, right: 0, margin: '0 auto', width: 'fit-content', top: 1520}} solid />
    </Backdrop>
  );
};

/* ---------- 5. Tools ---------- */
const TOOLS = [
  {icon: 'pickup', hue: 50, name: '淘宝取件', status: '2 件待取', red: true, side: 'l', y: 820},
  {icon: 'shower', hue: 220, name: '浴室预约', status: '空闲 4 间', side: 'r', y: 960},
  {icon: 'bolt', hue: 95, name: '电费查询', status: '38.6 度', side: 'l', y: 1170},
  {icon: 'todo', hue: 250, name: '超星待办', status: '3 项', red: true, side: 'r', y: 1310},
  {icon: 'pin', hue: 150, name: '模拟定位', status: '绘制路线', side: 'l', y: 1520},
  {icon: 'nfc', hue: 285, name: 'NFC 打卡', status: '离线重放', side: 'r', y: 1660},
];
export const Tools: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const e = spr(f, fps, 0, SOFT);
  const w = 540;
  return (
    <Backdrop color={C.page}>
      <Headline kicker="03" label="工具" icon="grid" hue={60} lines={[['校园零碎事，'], ['收进', {t: '一张清单', a: true}]]} />
      <Phone src="proto/light-tools.png" cx={540} cy={1330 + float(f, 6, 120)} w={w} ty={(1 - e) * 1000} scale={0.9 + e * 0.1} rx={(1 - e) * 20}
        dim={ramp(f, 30, 60, 0, 0.18)} />
      {TOOLS.map((t, k) => {
        const p = spr(f, fps, 30 + k * 7, BOUNCY);
        const left = t.side === 'l';
        return (
          <div key={k} style={{position: 'absolute', top: t.y, [left ? 'left' : 'right']: 48, display: 'flex', alignItems: 'center', gap: 20,
            background: C.surface, borderRadius: 36, padding: '18px 34px 18px 18px', boxShadow: SHADOW, fontFamily: CJK,
            transform: `translateY(${float(f, 7, 100, k * 23)}px) translateX(${(1 - p) * (left ? -60 : 60)}px) scale(${p})`,
            transformOrigin: left ? '0% 50%' : '100% 50%', opacity: Math.min(1, p * 2)}}>
            <div style={{width: 80, height: 80, borderRadius: 25, background: tint(t.hue), display: 'grid', placeItems: 'center'}}>
              <Icon k={t.icon} size={44} color={glyph(t.hue)} />
            </div>
            <div>
              <div style={{fontSize: 38, fontWeight: 750, color: C.ink, lineHeight: 1.2}}>{t.name}</div>
              <div style={{fontSize: 28, fontWeight: 650, color: t.red ? C.seal : C.ink2, lineHeight: 1.3}}>{t.status}</div>
            </div>
          </div>
        );
      })}
    </Backdrop>
  );
};

/* ---------- 6. Pickup ---------- */
export const Pickup: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const e = spr(f, fps, 0, SOFT);
  const w = 560;
  const cy = 1320 + float(f, 6, 120);
  const l1 = spr(f, fps, 46, SNAPPY);
  const l2 = spr(f, fps, 58, SNAPPY);
  const tap = ramp(f, 92, 122);
  const press = f >= 88 && f < 98 ? 0.97 : 1;
  const toast = spr(f, fps, 98, BOUNCY);
  return (
    <Backdrop color={C.page}>
      <BigType text="3-1-2046 B08-1173" top={1100} size={360} stroke="rgba(29,27,24,.06)" drift={-3.4} from={0} />
      <Headline kicker="04" label="淘宝取件" icon="pickup" hue={50} lines={[['取件码放大，'], ['点一下就', {t: '复制', a: true}]]} />
      <Phone src="proto/light-pickup-list.png" cx={540} cy={cy} w={w} tx={(1 - e) * -1000} ry={(1 - e) * 30} rotate={(1 - e) * -8}
        dim={Math.max(l1, l2) * 0.35} />
      <LiftOut src="proto/light-pickup-list.png" rect={{x: 25, y: 412, w: 356, h: 146, r: 24}} phone={{cx: 540, cy, w}}
        to={{cx: 540, cy: 990, scale: 1.5 * press, rotate: -3}} p={l1} shine={ramp(f, 62, 96)}>
        {tap > 0 && tap < 1 ? (
          <div style={{position: 'absolute', left: 100, top: 92, width: 200, height: 200, marginLeft: -100, marginTop: -100, borderRadius: '50%',
            background: 'rgba(29,27,24,.16)', transform: `scale(${0.1 + tap * 1.6})`, opacity: 1 - tap}} />
        ) : null}
      </LiftOut>
      <LiftOut src="proto/light-pickup-list.png" rect={{x: 25, y: 569, w: 356, h: 145, r: 24}} phone={{cx: 540, cy, w}}
        to={{cx: 540, cy: 1350, scale: 1.5, rotate: 2.5}} p={l2} shine={ramp(f, 72, 106)} />
      <div style={{position: 'absolute', left: 0, right: 0, top: 1590, display: 'flex', justifyContent: 'center'}}>
        <Chip icon="check" text="已复制 3-1-2046" p={toast} solid style={{position: 'relative'}} />
      </div>
    </Backdrop>
  );
};

/* ---------- 7. Bath ---------- */
export const Bath: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const e = spr(f, fps, 0, SOFT);
  const w = 560;
  const cy = 1320 + float(f, 6, 120);
  const nav = ramp(f, 46, 72);
  const lp = spr(f, fps, 84, {damping: 16, stiffness: 170, mass: 0.8});
  const bell = spr(f, fps, 112, BOUNCY);
  const ring = Math.sin(f * 0.9) * 14 * Math.max(0, 1 - ramp(f, 112, 150));
  return (
    <Backdrop color={C.page}>
      <BigType text="09:41 09:41 09:41" top={1100} size={380} stroke="rgba(29,27,24,.06)" drift={-3} from={-100} />
      <Headline kicker="05" label="浴室预约" icon="shower" hue={220} lines={[['空位一眼看清，'], ['到号', {t: '就提醒', a: true}]]} />
      <Phone src="proto/light-bath-devices.png" cx={540} cy={cy} w={w} ty={(1 - e) * 1000} rx={(1 - e) * 24} dim={lp * 0.35}>
        {nav > 0 ? (
          <>
            <div style={{position: 'absolute', inset: 0, background: `rgba(20,15,10,${0.25 * nav})`}} />
            <Img src={staticFile('proto/light-bath-ticket.png')} style={{position: 'absolute', inset: 0, width: '100%', height: '100%',
              transform: `translateX(${(1 - nav) * 100}%)`, boxShadow: '-20px 0 40px rgba(0,0,0,.18)'}} />
          </>
        ) : null}
      </Phone>
      <LiftOut src="proto/light-bath-ticket.png" rect={{x: 25, y: 154, w: 356, h: 205, r: 28}} phone={{cx: 540, cy, w}}
        to={{cx: 540, cy: 1060, scale: 1.45, rotate: -2}} p={lp} shine={ramp(f, 100, 136)} />
      <div style={{position: 'absolute', left: 0, right: 0, top: 1420, display: 'flex', justifyContent: 'center'}}>
        <div style={{position: 'relative', display: 'flex', alignItems: 'center', gap: 18, padding: '22px 36px 22px 24px', borderRadius: 999,
          background: C.surface, boxShadow: SHADOW, fontFamily: CJK, fontSize: 36, fontWeight: 650, color: C.ink,
          transform: `scale(${bell})`, opacity: Math.min(1, bell * 2.5)}}>
          <div style={{width: 64, height: 64, borderRadius: '50%', background: C.seal, display: 'grid', placeItems: 'center', transform: `rotate(${ring}deg)`}}>
            <Icon k="bell" size={38} color="#fff" stroke={2} />
          </div>
          轮到你了，10 分钟内到达
        </div>
      </div>
    </Backdrop>
  );
};

/* ---------- 10. Dark mode ---------- */
const TOGGLE = {x: 800, y: 250, w: 176, h: 96};
export const Dark: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const w = 560;
  const cy = 1320 + float(f, 6, 120);
  const knob = spr(f, fps, 28, SNAPPY);
  const tp = spr(f, fps, 2, BOUNCY);
  const reveal = ramp(f, 36, 84, 0, 1, EASE);
  const fan = spr(f, fps, 92, SOFT);
  const ox = TOGGLE.x + TOGGLE.w / 2;
  const oy = TOGGLE.y + TOGGLE.h / 2;
  const toggle = (dark: boolean) => (
    <div style={{position: 'absolute', left: TOGGLE.x, top: TOGGLE.y, width: TOGGLE.w, height: TOGGLE.h, borderRadius: 999,
      background: dark ? C.dsurface2 : C.surface, boxShadow: dark ? 'none' : SHADOW, transform: `scale(${tp})`}}>
      <div style={{position: 'absolute', top: 8, left: 8 + knob * (TOGGLE.w - TOGGLE.h), width: TOGGLE.h - 16, height: TOGGLE.h - 16, borderRadius: '50%',
        background: dark ? C.dink : C.ink, display: 'grid', placeItems: 'center'}}>
        <Icon k={knob > 0.5 ? 'moon' : 'sun'} size={44} color={dark ? C.dbg : '#fff'} stroke={2} />
      </div>
    </div>
  );
  return (
    <AbsoluteFill>
      <Backdrop color={C.page}>
        <Headline kicker="06" label="深色模式" icon="sun" hue={80} lines={[['暖白的纸，'], ['墨色的', {t: '字', a: true}]]} />
        <Phone src="proto/light-home.png" cx={540} cy={cy} w={w} />
        {toggle(false)}
      </Backdrop>
      <AbsoluteFill style={{clipPath: `circle(${reveal * 2300}px at ${ox}px ${oy}px)`}}>
        <Backdrop color={C.dpage} glow="rgba(255,255,255,.07)">
          <Headline dark kicker="06" label="深色模式" icon="moon" hue={280} delay={44}
            lines={[['夜里看课表，'], ['也不', {t: '刺眼', a: true}]]} />
          <Phone src="proto/dark-pickup-list.png" cx={540} cy={cy + 40} w={w} tx={-300 * fan} rotate={-9 * fan} scale={1 - 0.14 * fan} opacity={fan > 0.02 ? 1 : 0} />
          <Phone src="proto/dark-bath-ticket.png" cx={540} cy={cy + 40} w={w} tx={300 * fan} rotate={9 * fan} scale={1 - 0.14 * fan} opacity={fan > 0.02 ? 1 : 0} />
          <Phone src="proto/dark-home.png" cx={540} cy={cy} w={w} />
          {toggle(true)}
        </Backdrop>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

/* ---------- 11. Outro ---------- */
export const Outro: React.FC = () => {
  const f = useT();
  const {fps} = useVideoConfig();
  const drop = spr(f, fps, 0, {damping: 14, stiffness: 240, mass: 0.9});
  const hit = 8;
  const since = f - hit;
  const shake = since > 0 ? 12 * Math.exp(-since / 6) * Math.sin(since * 2.4) : 0;
  const rp = ramp(f, hit, hit + 40);
  const cta = spr(f, fps, 62, BOUNCY);
  const glint = ramp(f, 96, 140);
  return (
    <Backdrop color={C.seal} glow="rgba(255,255,255,.16)">
      <BigType text="QLIT QLIT QLIT" top={1500} size={420} stroke="rgba(255,255,255,.12)" drift={-2.6} from={0} />
      <AbsoluteFill style={{transform: `translateY(${shake}px)`}}>
        {rp > 0 && rp < 1 ? (
          <div style={{position: 'absolute', left: 540, top: 620, width: 460, height: 460, marginLeft: -230, marginTop: -230, borderRadius: '50%',
            border: `${6 * (1 - rp) + 1}px solid #fff`, transform: `scale(${0.9 + rp * 1.6})`, opacity: (1 - rp) * 0.55}} />
        ) : null}
        <div style={{position: 'absolute', left: 540, top: 620, width: 440, height: 440, marginLeft: -220, marginTop: -220,
          transform: `scale(${interpolate(drop, [0, 1], [3, 1])}) rotate(${interpolate(drop, [0, 1], [24, 0])}deg)`, opacity: ramp(f, 0, 5)}}>
          <AppIcon size={400} shape="squircle" style={{margin: 20, boxShadow: '0 50px 90px -30px rgba(60,0,0,.6), 0 0 0 6px rgba(255,255,255,.25)'}} />
        </div>
        <RiseLine top={960} size={150} delay={24} parts={[{t: 'QLIT', font: NUM, color: '#fff', weight: 700}, {t: ' 小助手', color: '#fff'}]} />
        <RiseLine top={1165} size={44} delay={40} step={1} dur={18} parts={[{t: '装上它，明天第一节课不用再翻截图', color: 'rgba(255,255,255,.86)', weight: 550}]} />
        <div style={{position: 'absolute', left: 0, right: 0, top: 1320, display: 'flex', justifyContent: 'center'}}>
          <div style={{position: 'relative', overflow: 'hidden', display: 'flex', alignItems: 'center', gap: 18, padding: '30px 56px 30px 46px', borderRadius: 999,
            background: '#fff', color: C.seal, fontFamily: CJK, fontSize: 48, fontWeight: 800, boxShadow: '0 30px 60px -20px rgba(60,0,0,.55)',
            transform: `scale(${cta})`, opacity: Math.min(1, cta * 2.5)}}>
            <Icon k="download" size={52} color={C.seal} stroke={2.4} />
            下载 Android 版
            {glint > 0 && glint < 1 ? (
              <div style={{position: 'absolute', top: '-50%', bottom: '-50%', width: '30%', left: `${lerp(-40, 130, glint)}%`, transform: 'rotate(20deg)',
                background: 'linear-gradient(90deg, transparent, rgba(200,22,29,.18), transparent)'}} />
            ) : null}
          </div>
        </div>
        <div style={{position: 'absolute', left: 0, right: 0, top: 1490, textAlign: 'center', fontFamily: CJK, fontSize: 34, fontWeight: 550,
          color: 'rgba(255,255,255,.72)', opacity: ramp(f, 80, 100)}}>
          <span style={{fontFamily: NUM}}>V2.0.3</span> · Android <span style={{fontFamily: NUM}}>12</span> 及以上 · <span style={{fontFamily: NUM}}>0</span> 广告
        </div>
      </AbsoluteFill>
    </Backdrop>
  );
};
