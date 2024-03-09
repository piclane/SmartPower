import React, {useEffect, useImperativeHandle, useState} from "react";
import './ProcessingLed.css';
import {DateTime} from "luxon";
import {printf} from "fast-printf";

interface ProcessingLedProps {
  style?: React.CSSProperties;
}

export interface ProcessingLedMethods {
  flush(): void;
}

export const ProcessingLed = React.forwardRef<ProcessingLedMethods, ProcessingLedProps>((props, ref) => {
  const [active, setActive] = useState(false);
  const [lastActivatedAt, setLastActivatedAt] = useState<DateTime>(DateTime.now());

  useImperativeHandle(ref, () => ({
    flush() {
      setActive(true);
      setLastActivatedAt(DateTime.now());
      setTimeout(() => {
        setActive(false);
      }, 33);
    }
  }));

  return <>
    <div className={`processing-led ${active ? 'active' : 'inactive'}`} style={props.style}></div>
    <ElapsedTime lastActivatedAt={lastActivatedAt} />
  </>;
});

interface ElapsedTimeProps {
  lastActivatedAt: DateTime;
}

const ElapsedTime = (props: ElapsedTimeProps) => {
  const [elapsedTime, setElapsedTime] = useState(0);
  const [now, setNow] = useState(0);

  useEffect(() => {
    const timerId = setInterval(() => {
      setNow(Date.now());
    }, 132);
    return () => clearInterval(timerId);
  }, []);

  useEffect(() => {
    setElapsedTime(props.lastActivatedAt.diffNow().negate().as('milliseconds') / 1000);
  }, [now, props.lastActivatedAt]);

  if(elapsedTime > 5) {
    return (
      <span className="elapsed-time">
        {printf('%.1f', elapsedTime)}
      </span>
    );
  } else {
    return null;
  }
};
