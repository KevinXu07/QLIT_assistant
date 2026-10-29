import {Easing, interpolate, spring, SpringConfig, useCurrentFrame} from 'remotion';

export const C = {
  page: '#E3E0DA', bg: '#F3F1EC', surface: '#FFFFFF', surface2: '#EBE8E2',
  ink: '#1D1B18', ink2: '#6B665E', ink3: '#A39D93', seal: '#C8161D',
  dpage: '#0A0A09', dbg: '#141312', dsurface: '#1F1D1B', dsurface2: '#2B2825',
  dink: '#F2EFEA', dink2: '#A8A29A', dink3: '#6E6962', dseal: '#E0383F',
};
export const CJK = '"Noto Sans SC", "Microsoft YaHei", sans-serif';
export const NUM = '"Barlow SC", "Noto Sans SC", sans-serif';

export const tint = (h: number, dark = false) => (dark ? `oklch(0.3 0.055 ${h})` : `oklch(0.93 0.05 ${h})`);
export const glyph = (h: number, dark = false) => (dark ? `oklch(0.8 0.12 ${h})` : `oklch(0.52 0.15 ${h})`);
export const courseBg = (h: number) => `oklch(0.935 0.042 ${h})`;
export const courseBar = (h: number) => `oklch(0.64 0.13 ${h})`;
export const courseText = (h: number) => `oklch(0.36 0.085 ${h})`;

export const ICONS: Record<string, string> = {
  pickup: '<path d="M3.5 7.5 12 3.5l8.5 4v9L12 20.5l-8.5-4z"/><path d="M3.5 7.5 12 11.5l8.5-4M12 11.5v9"/>',
  shower: '<path d="M5 12.5a7 7 0 0 1 14 0z"/><path d="M12 5.5V3.5M8 16.5v1M12 16.5v1.5M16 16.5v1M10 20.5v.5M14 20.5v.5"/>',
  bolt: '<path d="M13 2.5 5 13.5h6l-1 8 8-11h-6z"/>',
  todo: '<path d="M4.5 5A1.5 1.5 0 0 1 6 3.5h13.5v14H6a1.5 1.5 0 0 0-1.5 1.5z"/><path d="M4.5 19A1.5 1.5 0 0 0 6 20.5h13.5v-3"/><path d="m9 10.5 2 2 4-4"/>',
  pin: '<path d="M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z"/><circle cx="12" cy="10" r="2.4"/>',
  nfc: '<path d="M7.5 8a5.5 5.5 0 0 1 0 8M11 5.5a9 9 0 0 1 0 13M14.5 3a12.5 12.5 0 0 1 0 18"/>',
  luggage: '<rect x="5" y="7.5" width="14" height="12" rx="2.5"/><path d="M9 7.5V5a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 5v2.5M9 11v5M15 11v5"/>',
  wallet: '<rect x="3.5" y="6" width="17" height="13" rx="3"/><path d="M3.5 10h17M16 14.5h1.5"/>',
  home: '<path d="M3.5 10.5 12 3.5l8.5 7V19a1.5 1.5 0 0 1-1.5 1.5h-4.5V15h-5v5.5H5A1.5 1.5 0 0 1 3.5 19z"/>',
  week: '<rect x="3.5" y="4.5" width="17" height="16" rx="3.5"/><path d="M3.5 9.5h17M8.5 2.5v4M15.5 2.5v4M8 13.5h2M14 13.5h2M8 17h2"/>',
  grid: '<rect x="3.5" y="3.5" width="7" height="7" rx="2.2"/><rect x="13.5" y="3.5" width="7" height="7" rx="2.2"/><rect x="3.5" y="13.5" width="7" height="7" rx="2.2"/><circle cx="17" cy="17" r="3.6"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  copy: '<rect x="8.5" y="8.5" width="11" height="11" rx="2.5"/><path d="M15.5 8.5V6A1.5 1.5 0 0 0 14 4.5H6A1.5 1.5 0 0 0 4.5 6v8A1.5 1.5 0 0 0 6 15.5h2.5"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  bell: '<path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/>',
  moon: '<path d="M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z"/>',
  route: '<circle cx="6" cy="18" r="2"/><circle cx="18" cy="6" r="2"/><path d="M8 18h7a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h7"/>',
  download: '<path d="M12 4v11M7 10.5l5 5 5-5M5 19.5h14"/>',
};

export const EASE = Easing.bezier(0.32, 0.72, 0, 1);
export const EASE_IN = Easing.bezier(0.55, 0, 1, 0.45);
export const EASE_IO = Easing.bezier(0.65, 0, 0.35, 1);
export const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
export const ramp = (f: number, a: number, b: number, from = 0, to = 1, easing = EASE) =>
  interpolate(f, [a, b], [from, to], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing});
export const SOFT: Partial<SpringConfig> = {damping: 20, stiffness: 110, mass: 1};
export const SNAPPY: Partial<SpringConfig> = {damping: 15, stiffness: 210, mass: 0.7};
export const BOUNCY: Partial<SpringConfig> = {damping: 10, stiffness: 200, mass: 0.7};
export const spr = (f: number, fps: number, delay = 0, config: Partial<SpringConfig> = SNAPPY) =>
  spring({frame: f - delay, fps, config});

/** Scene motion runs this much slower than real frames. */
export const SLOW = 1.2;
export const useT = () => useCurrentFrame() / SLOW;
