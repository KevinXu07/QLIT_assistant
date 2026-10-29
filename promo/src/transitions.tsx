import React from 'react';
import {AbsoluteFill} from 'remotion';
import {linearTiming, TransitionPresentation, TransitionPresentationComponentProps} from '@remotion/transitions';
import {C, EASE_IO, lerp} from './theme';

export const timing = (d: number) => linearTiming({durationInFrames: d, easing: EASE_IO});

type PushProps = {dir: 'up' | 'left'};
const PushComp: React.FC<TransitionPresentationComponentProps<PushProps>> = ({children, presentationDirection, presentationProgress: p, passedProps}) => {
  const h = passedProps.dir === 'left';
  if (presentationDirection === 'exiting') {
    const t = h ? `translateX(${-p * 30}%)` : `translateY(${-p * 24}%)`;
    return (
      <AbsoluteFill style={{transform: `${t} scale(${1 - 0.08 * p})`}}>
        {children}
        <AbsoluteFill style={{background: `rgba(14,10,8,${0.5 * p})`}} />
      </AbsoluteFill>
    );
  }
  const t = h ? `translateX(${(1 - p) * 100}%)` : `translateY(${(1 - p) * 100}%)`;
  return (
    <AbsoluteFill style={{transform: t, overflow: 'hidden', borderRadius: 72 * (1 - p), boxShadow: `0 0 140px rgba(0,0,0,${0.4 * (1 - p)})`}}>
      {children}
    </AbsoluteFill>
  );
};
export const push = (dir: PushProps['dir']): TransitionPresentation<PushProps> => ({component: PushComp, props: {dir}});

type CircleProps = {x: number; y: number};
const CircleComp: React.FC<TransitionPresentationComponentProps<CircleProps>> = ({children, presentationDirection, presentationProgress: p, passedProps}) => {
  if (presentationDirection === 'exiting') {
    return <AbsoluteFill style={{transform: `scale(${1 + 0.08 * p})`}}>{children}</AbsoluteFill>;
  }
  return <AbsoluteFill style={{clipPath: `circle(${p * 2300}px at ${passedProps.x}px ${passedProps.y}px)`}}>{children}</AbsoluteFill>;
};
export const circle = (x: number, y: number): TransitionPresentation<CircleProps> => ({component: CircleComp, props: {x, y}});

/** A skewed red band sweeps across; the new scene is revealed behind it. */
type SweepProps = {color: string};
const SweepComp: React.FC<TransitionPresentationComponentProps<SweepProps>> = ({children, presentationDirection, presentationProgress: p, passedProps}) => {
  if (presentationDirection === 'exiting') {
    return <AbsoluteFill style={{transform: `translateX(${-Math.min(p, 0.5) * 16}%)`}}>{children}</AbsoluteFill>;
  }
  const covering = p < 0.5;
  const x = covering ? lerp(130, 0, p * 2) : lerp(0, -130, (p - 0.5) * 2);
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{opacity: covering ? 0 : 1, transform: `translateX(${covering ? 0 : (1 - (p - 0.5) * 2) * 12}%)`}}>{children}</AbsoluteFill>
      <AbsoluteFill style={{transform: `translateX(${x}%) skewX(-14deg) scaleX(1.35)`, background: passedProps.color}}>
        <AbsoluteFill style={{left: covering ? 'auto' : '86%', right: covering ? '86%' : 'auto', width: '14%', background: C.ink, opacity: 0.9}} />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
export const sweep = (color: string = C.seal): TransitionPresentation<SweepProps> => ({component: SweepComp, props: {color}});
