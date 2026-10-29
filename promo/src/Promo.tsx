import React from 'react';
import {Html5Audio, staticFile} from 'remotion';
import {TransitionSeries} from '@remotion/transitions';
import {C} from './theme';
import {circle, push, sweep, timing} from './transitions';
import {Bath, Dark, Intro, Kinetic, Outro, Pickup, Today, Tools, Week} from './scenes';
import timeline from './timeline.json';

// Scene cuts sit on the beat grid of public/music.wav; music/make-music.mjs reads the same timeline.
const SCENES: React.FC[] = [Intro, Kinetic, Today, Week, Tools, Pickup, Bath, Dark, Outro];
const PRESENTATIONS = [circle(540, 760), sweep(C.seal), push('left'), push('up'), push('left'), push('up'), sweep(C.ink), circle(540, 960)];

const starts = [0, ...timeline.cuts.map((c, i) => Math.round(c * timeline.fps) - timeline.trans[i] / 2)];
const durations = SCENES.map((_, i) => (i < SCENES.length - 1 ? starts[i + 1] + timeline.trans[i] - starts[i] : timeline.total - starts[i]));

export const PROMO_FRAMES = timeline.total;

export const Promo: React.FC = () => (
  <>
    <TransitionSeries>
      {SCENES.flatMap((Scene, i) => {
        const items = [
          <TransitionSeries.Sequence key={`s${i}`} durationInFrames={durations[i]}>
            <Scene />
          </TransitionSeries.Sequence>,
        ];
        if (i < PRESENTATIONS.length) {
          items.push(<TransitionSeries.Transition key={`t${i}`} presentation={PRESENTATIONS[i] as never} timing={timing(timeline.trans[i])} />);
        }
        return items;
      })}
    </TransitionSeries>
    <Html5Audio src={staticFile('music.wav')} />
  </>
);
