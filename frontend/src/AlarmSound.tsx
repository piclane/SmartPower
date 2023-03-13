import React, {useEffect, useRef, useState} from "react";

interface AlarmSoundProps {
  triggered: boolean;
}

export const AlarmSound = ({triggered}: AlarmSoundProps) => {
  const ref = useRef<HTMLAudioElement>(null);
  const [canPlay, setCanPlay] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if(!el || !canPlay) {
      return;
    }
    if(triggered) {
      el.currentTime = 0;
      el.muted = false;
      el.play().catch(e => {
        console.log('サイト設定から「音声」を「許可する」に変更してください');
      });
    } else {
      el.pause();
    }
  }, [ref, canPlay, triggered]);
  useEffect(() => {
    const el = ref.current;
    if(!el || canPlay) {
      return;
    }
    el.addEventListener('canplaythrough', function handler() {
      el.removeEventListener('canplaythrough', handler);
      setCanPlay(true);
    });
  }, [ref, canPlay]);

  return (
      <audio src="./hell-yeah.mp3" ref={ref} loop preload="auto" />
  );
};
