import {continueRender, delayRender, staticFile} from 'remotion';

const handle = delayRender('Loading fonts');
const faces = [
  new FontFace('Barlow SC', `url(${staticFile('fonts/barlow-500.ttf')})`, {weight: '500'}),
  new FontFace('Barlow SC', `url(${staticFile('fonts/barlow-600.ttf')})`, {weight: '600'}),
  new FontFace('Barlow SC', `url(${staticFile('fonts/barlow-700.ttf')})`, {weight: '700'}),
  new FontFace('Noto Sans SC', `url(${staticFile('fonts/NotoSansSC-VF.ttf')})`, {weight: '100 900'}),
];
Promise.all(faces.map((f) => f.load()))
  .then((loaded) => {
    loaded.forEach((f) => document.fonts.add(f));
    continueRender(handle);
  })
  .catch((err) => {
    console.error(err);
    continueRender(handle);
  });
