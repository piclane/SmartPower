import React, {useImperativeHandle, useState} from "react";
import './ProcessingLed.css';

interface ProcessingLedProps {
  style?: React.CSSProperties;
}

export interface ProcessingLedMethods {
  flush(): void;
}

export const ProcessingLed = React.forwardRef<ProcessingLedMethods, ProcessingLedProps>((props, ref) => {
  const [active, setActive] = useState(false);
  useImperativeHandle(ref, () => ({
    flush() {
      setActive(true);
      setTimeout(() => {
        setActive(false);
      }, 10);
    }
  }));
  return <>
    <div className={`processing-led ${active ? 'active' : 'inactive'}`} style={props.style}></div>
  </>;
});
