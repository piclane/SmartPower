import {CSSProperties, useEffect, useMemo, useState} from "react";
import useInterval from "use-interval-hook";
import {DateTime} from "luxon";
import './DigitalClock.scss';

interface Props {
  style: CSSProperties;
}

export const DigitalClock = (props: Props) => {
  const [epocSec, setEpocSec] = useState<number>(0);
  const {activate, stop} = useInterval({
    interval: 200,
    callback: () => {
      setEpocSec(Math.floor(Date.now() / 1000));
    },
  });
  useEffect(() => {
    activate();
    return () => stop();
  }, [activate, stop]);
  const hhmm = useMemo(() => DateTime.fromSeconds(epocSec).toFormat("HH:mm"), [epocSec]);
  const ss = useMemo(() => DateTime.fromSeconds(epocSec).toFormat(":ss"), [epocSec]);
  return (
      <div style={props.style} className="digital-clock">
        <span className="hhmm">{ hhmm }</span>
        <span className="ss">{ ss }</span>
      </div>
  );
};
