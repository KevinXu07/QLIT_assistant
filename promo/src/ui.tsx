import React, {CSSProperties, useId} from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame} from 'remotion';
import {C, CJK, ICONS, NUM, glyph, lerp, ramp, tint, useT} from './theme';

export const Icon: React.FC<{k: string; size: number; color: string; stroke?: number; style?: CSSProperties}> = ({k, size, color, stroke = 1.8, style}) => (
  <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} strokeWidth={stroke}
    strokeLinecap="round" strokeLinejoin="round" style={style} dangerouslySetInnerHTML={{__html: ICONS[k]}} />
);

/**
 * The launcher icon (app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp).
 * Its artwork sits in the centre two thirds of the canvas, the adaptive-icon safe zone,
 * and the seal ring spans about 95% of that area.
 */
export const AppIcon: React.FC<{size: number; shape: 'seal' | 'squircle'; style?: CSSProperties}> = ({size, shape, style}) => {
  const art = shape === 'seal' ? size / 0.95 : size;
  const img = art * 1.5;
  return (
    <div style={{position: 'relative', width: size, height: size, overflow: 'hidden', background: '#fff',
      borderRadius: shape === 'seal' ? '50%' : size * 0.23, ...style}}>
      <Img src={staticFile('app-icon.webp')} style={{position: 'absolute', width: img, height: img, left: (size - img) / 2, top: (size - img) / 2, maxWidth: 'none'}} />
    </div>
  );
};

export const Grain: React.FC<{opacity?: number}> = ({opacity = 0.09}) => {
  const f = useCurrentFrame();
  const id = useId().replace(/[^a-zA-Z0-9]/g, '');
  const seed = Math.floor(f / 2) % 12;
  return (
    <AbsoluteFill style={{pointerEvents: 'none', opacity, mixBlendMode: 'soft-light'}}>
      <svg width="100%" height="100%">
        <filter id={`g${id}`}>
          <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves={2} seed={seed} stitchTiles="stitch" />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter={`url(#g${id})`} />
      </svg>
    </AbsoluteFill>
  );
};

export const Backdrop: React.FC<{color: string; glow?: string; children?: React.ReactNode}> = ({color, glow = 'rgba(255,255,255,.55)', children}) => (
  <AbsoluteFill style={{background: color}}>
    <AbsoluteFill style={{background: `radial-gradient(120% 70% at 50% 18%, ${glow}, transparent 62%)`}} />
    {children}
    <Grain />
  </AbsoluteFill>
);

export const BigType: React.FC<{text: string; top: number; size?: number; stroke: string; drift?: number; from?: number}> = ({text, top, size = 420, stroke, drift = -2.2, from = 0}) => {
  const f = useT();
  return (
    <div style={{position: 'absolute', top, left: from + f * drift, fontFamily: NUM, fontWeight: 700, fontSize: size, lineHeight: 1,
      color: 'transparent', WebkitTextStroke: `3px ${stroke}`, whiteSpace: 'nowrap', letterSpacing: -4}}>{text}</div>
  );
};

type Seg = string | {t: string; a?: boolean};
export const Headline: React.FC<{
  kicker: string; label: string; icon?: string; hue?: number; lines: Seg[][]; sub?: string;
  dark?: boolean; delay?: number; top?: number; size?: number;
}> = ({kicker, label, icon, hue = 0, lines, sub, dark = false, delay = 0, top = 140, size = 112}) => {
  const f = useT();
  const ink = dark ? C.dink : C.ink;
  const ink2 = dark ? C.dink2 : C.ink2;
  const seal = dark ? C.dseal : C.seal;
  const kp = ramp(f, delay, delay + 16);
  let idx = 0;
  const lineEls = lines.map((line, li) => (
    <div key={li} style={{fontSize: size, fontWeight: 850, lineHeight: 1.24, color: ink, whiteSpace: 'nowrap', overflow: 'hidden', paddingBottom: 6, marginBottom: -6}}>
      {line.map((seg, si) => {
        const s = typeof seg === 'string' ? {t: seg} : seg;
        const chars = [...s.t].map((ch) => {
          const i = idx++;
          const t0 = delay + 5 + i * 1.6;
          const p = ramp(f, t0, t0 + 20);
          return (
            <span key={i} style={{display: 'inline-block', whiteSpace: 'pre', transform: `translateY(${(1 - p) * 108}%) rotate(${(1 - p) * 7}deg)`, transformOrigin: '0 100%'}}>{ch}</span>
          );
        });
        const mark = s.a ? ramp(f, delay + 5 + idx * 1.6 + 2, delay + 5 + idx * 1.6 + 20) : 0;
        return (
          <span key={si} style={{position: 'relative', display: 'inline-block', color: s.a ? seal : ink}}>
            {s.a ? (
              <span style={{position: 'absolute', left: -4, right: -4, bottom: '0.1em', height: '0.26em', borderRadius: 8,
                background: dark ? 'rgba(230,70,76,.24)' : 'rgba(200,22,29,.13)', transform: `scaleX(${mark})`, transformOrigin: '0 50%'}} />
            ) : null}
            <span style={{position: 'relative'}}>{chars}</span>
          </span>
        );
      })}
    </div>
  ));
  const st = delay + 5 + idx * 1.6 + 2;
  const sp = ramp(f, st, st + 20);
  return (
    <div style={{position: 'absolute', left: 84, right: 84, top, fontFamily: CJK}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 18, opacity: kp, transform: `translateX(${(1 - kp) * -30}px)`}}>
        {icon ? (
          <div style={{width: 64, height: 64, borderRadius: 20, background: tint(hue, dark), display: 'grid', placeItems: 'center'}}>
            <Icon k={icon} size={36} color={glyph(hue, dark)} />
          </div>
        ) : null}
        <span style={{fontFamily: NUM, fontWeight: 600, fontSize: 36, color: dark ? C.dink3 : C.ink3}}>{kicker}</span>
        <span style={{fontSize: 36, fontWeight: 600, color: ink2}}>{label}</span>
      </div>
      <div style={{marginTop: 20}}>{lineEls}</div>
      {sub ? (
        <div style={{marginTop: 20, fontSize: 40, fontWeight: 500, color: ink2, opacity: sp, transform: `translateY(${(1 - sp) * 22}px)`}}>{sub}</div>
      ) : null}
    </div>
  );
};

export const RATIO = 844 / 390;
export type PhoneProps = {
  src: string; cx: number; cy: number; w: number; scale?: number; rotate?: number; rx?: number; ry?: number;
  tx?: number; ty?: number; opacity?: number; dim?: number; z?: number; children?: React.ReactNode;
};
export const Phone: React.FC<PhoneProps> = ({src, cx, cy, w, scale = 1, rotate = 0, rx = 0, ry = 0, tx = 0, ty = 0, opacity = 1, dim = 0, z, children}) => {
  const h = w * RATIO;
  const b = w * 0.032;
  const r = (w * 52) / 390;
  return (
    <div style={{
      position: 'absolute', left: cx, top: cy, width: w + 2 * b, height: h + 2 * b, marginLeft: -(w / 2 + b), marginTop: -(h / 2 + b),
      transform: `translate(${tx}px, ${ty}px) perspective(2400px) rotateX(${rx}deg) rotateY(${ry}deg) rotate(${rotate}deg) scale(${scale})`,
      opacity, zIndex: z, borderRadius: r + b, padding: b, background: 'linear-gradient(150deg, #34312d 0%, #0b0a09 45%, #1c1a18 100%)',
      boxShadow: '0 70px 120px -30px rgba(30,18,8,.5), 0 24px 44px -20px rgba(30,18,8,.45), inset 0 0 0 2px rgba(255,255,255,.1)',
    }}>
      <div style={{position: 'relative', width: w, height: h, borderRadius: r, overflow: 'hidden', background: '#000'}}>
        <Img src={staticFile(src)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%'}} />
        {dim > 0 ? <div style={{position: 'absolute', inset: 0, background: `rgba(10,8,6,${dim})`}} /> : null}
        {children}
      </div>
    </div>
  );
};

export type Rect = {x: number; y: number; w: number; h: number; r: number};
/** Copies a region of the phone screenshot and lifts it towards the viewer. */
export const LiftOut: React.FC<{
  src: string; rect: Rect; phone: {cx: number; cy: number; w: number}; to: {cx: number; cy: number; scale: number; rotate?: number};
  p: number; shine?: number; children?: React.ReactNode;
}> = ({src, rect, phone, to, p, shine = 0, children}) => {
  if (p <= 0.001) return null;
  const k = phone.w / 390;
  const left = phone.cx - phone.w / 2;
  const top = phone.cy - (phone.w * RATIO) / 2;
  const fx = left + (rect.x + rect.w / 2) * k;
  const fy = top + (rect.y + rect.h / 2) * k;
  const cx = lerp(fx, to.cx, p);
  const cy = lerp(fy, to.cy, p);
  const s = lerp(1, to.scale, p);
  const rot = (to.rotate ?? 0) * p;
  const sh = Math.min(1, p);
  const ghost = Math.min(1, p * 3);
  return (
    <>
    <div style={{position: 'absolute', left: fx, top: fy, width: rect.w * k, height: rect.h * k, marginLeft: (-rect.w * k) / 2, marginTop: (-rect.h * k) / 2,
      borderRadius: rect.r * k, background: `rgba(24,18,12,${0.22 * ghost})`, boxShadow: `inset 0 2px 10px rgba(0,0,0,${0.25 * ghost})`}} />
    <div style={{
      position: 'absolute', left: cx, top: cy, width: rect.w * k, height: rect.h * k, marginLeft: (-rect.w * k) / 2, marginTop: (-rect.h * k) / 2,
      transform: `rotate(${rot}deg) scale(${s})`, borderRadius: rect.r * k, overflow: 'hidden',
      boxShadow: `0 ${60 * sh}px ${110 * sh}px -${20 * sh}px rgba(30,18,8,${0.55 * sh}), 0 ${18 * sh}px ${30 * sh}px -${12 * sh}px rgba(30,18,8,${0.35 * sh})`,
    }}>
      <Img src={staticFile(src)} style={{position: 'absolute', left: -rect.x * k, top: -rect.y * k, width: 390 * k, height: 844 * k, maxWidth: 'none'}} />
      {shine > 0 && shine < 1 ? (
        <div style={{position: 'absolute', top: '-40%', bottom: '-40%', width: '45%', left: `${lerp(-60, 130, shine)}%`,
          transform: 'rotate(18deg)', background: 'linear-gradient(90deg, transparent, rgba(255,255,255,.38), transparent)'}} />
      ) : null}
      {children}
    </div>
    </>
  );
};

export const Chip: React.FC<{icon?: string; text: string; p: number; dark?: boolean; solid?: boolean; style?: CSSProperties; origin?: string}> = ({icon, text, p, dark = false, solid = false, style, origin = '50% 50%'}) => {
  const bg = solid ? (dark ? C.dink : C.ink) : dark ? C.dsurface2 : C.surface;
  const fg = solid ? (dark ? C.dbg : '#fff') : dark ? C.dink : C.ink;
  return (
    <div style={{
      position: 'absolute', display: 'flex', alignItems: 'center', gap: 14, padding: '20px 32px 20px 26px', borderRadius: 999,
      background: bg, color: fg, fontFamily: CJK, fontSize: 36, fontWeight: 650, whiteSpace: 'nowrap',
      boxShadow: dark ? '0 20px 40px -14px rgba(0,0,0,.6)' : '0 22px 44px -16px rgba(40,25,10,.35), 0 2px 6px rgba(40,25,10,.08)',
      transform: `scale(${Math.max(0, p)})`, transformOrigin: origin, opacity: Math.min(1, p * 2.5), ...style,
    }}>
      {icon ? <Icon k={icon} size={40} color={fg} stroke={2} /> : null}
      {text}
    </div>
  );
};
