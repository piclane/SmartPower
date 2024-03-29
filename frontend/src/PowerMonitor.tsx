import gql from "graphql-tag";
import {useQuery, useSubscription} from "@apollo/client";
import React, {useEffect, useRef, useState} from "react";
import {amber, blue, deepOrange, orange} from 'material-ui-colors'
import {Cell, Pie, PieChart} from "recharts";
import './PowerMonitor.scss';
import {NumericFormat} from "react-number-format";
import {ProcessingLed, ProcessingLedMethods} from "@/ProcessingLed";
import {AlarmSound} from "@/AlarmSound";

interface Instantaneous {
  power: number;
  current: Current;
}

interface Current {
  sum: number;
  rPhase: number;
  tPhase: number;
}

interface PowerSource {
  ratedCurrentA: number;
  wireCount: 2 | 3;
}

const defaultInstantaneous: Instantaneous = {
  power: 0,
  current: {
    sum: 0,
    tPhase: 0,
    rPhase: 0,
  },
};

const defaultPowerSource: PowerSource = {
  ratedCurrentA: 60,
  wireCount: 3,
};

const query = gql`
    subscription SmartPower {
        instantaneous {
            power
            current {
                sum
                rPhase
                tPhase
            }
        }
    }
`;

const initialQuery = gql`
    query SmartPowerInitial {
        powerSource {
            ratedCurrentA
            wireCount
        }
        instantaneous {
            power
            current {
                sum
                rPhase
                tPhase
            }
        }
    }
`;

/**
 * 使用量の色を計算します
 *
 * @param ratio パーセント
 */
function buildColor(ratio: number): string {
  const percent = ratio * 100;
  if (percent < 70) {
    // @ts-ignore
    return blue[Math.floor(5 + percent / 70 * 4) * 100];
  } else if (70 <= percent && percent < 80) {
    // @ts-ignore
    return amber[Math.floor(5 + (percent - 70) / 10 * 4) * 100];
  } else if (80 <= percent && percent < 90) {
    // @ts-ignore
    return orange[Math.floor(5 + (percent - 80) / 10 * 4) * 100];
  } else {
    // @ts-ignore
    return deepOrange[Math.floor(5 + (percent - 90) / 10 * 4) * 100];
  }
}

interface RendererProps {
  powerSource: PowerSource;
  instantaneous: Instantaneous;
}

type PieChartProps = ConstructorParameters<typeof PieChart>[0];

const BasePieChart = ({children, ...pcProps}: PieChartProps) => (
    <PieChart {...pcProps}>
      <defs>
        <filter id="filter_0">
          <feOffset dx="0" dy="3"></feOffset>
          <feGaussianBlur result="offset-blur" stdDeviation="5"></feGaussianBlur>
          <feComposite operator="out" in="SourceGraphic" in2="offset-blur" result="inverse"></feComposite>
          <feFlood floodColor="black" floodOpacity="0.4" result="color"></feFlood>
          <feComposite operator="in" in="color" in2="inverse" result="shadow"></feComposite>
          <feComposite operator="over" in="shadow" in2="SourceGraphic"></feComposite>
        </filter>
      </defs>
      {children}
    </PieChart>
);

interface PieChartRendererProps extends PieChartProps {
  powerSource: PowerSource;
  instantaneous: Instantaneous;
}

const TwoWirePieChart = React.memo(({powerSource, instantaneous, ...pcProps}: PieChartRendererProps) => {
  const current: Current = instantaneous.current;
  const rChartDataValues = [
    {name: 'used', value: current.rPhase, color: buildColor(current.rPhase / powerSource.ratedCurrentA)},
    {name: 'free', value: powerSource.ratedCurrentA - current.rPhase, color: '#edebeb', filter: 'url(#filter_0)'},
  ];
  return (
      <BasePieChart {...pcProps}>
        {/** R相 */}
        <Pie
            data={rChartDataValues}
            dataKey="value"
            cx="50%"
            cy="50%"
            startAngle={220}
            endAngle={-40}
            innerRadius={150}
            outerRadius={190}
            paddingAngle={0}
            isAnimationActive={true}
        >
          {rChartDataValues.map(cd => (
              <Cell
                  key={cd.name}
                  fill={cd.color}
                  stroke="0"
                  filter={cd.filter}
              />
          ))}
        </Pie>
      </BasePieChart>
  );
});

const ThreeWirePieChart = React.memo(({powerSource, instantaneous, ...pcProps}: PieChartRendererProps) => {
  const current: Current = instantaneous.current;
  const rChartDataValues = [
    {name: 'used', value: current.rPhase, color: buildColor(current.rPhase / powerSource.ratedCurrentA)},
    {name: 'free', value: powerSource.ratedCurrentA - current.rPhase, color: '#edebeb', filter: 'url(#filter_0)'},
  ];
  const tChartDataValues = [
    {name: 'used', value: current.tPhase, color: buildColor(current.tPhase / powerSource.ratedCurrentA)},
    {name: 'free', value: powerSource.ratedCurrentA - current.tPhase, color: '#edebeb', filter: 'url(#filter_0)'},
  ];

  return (
      <BasePieChart {...pcProps}>
        {/** R相 */}
        <Pie
            data={rChartDataValues}
            dataKey="value"
            cx="50%"
            cy="50%"
            startAngle={265}
            endAngle={95}
            innerRadius={150}
            outerRadius={190}
            paddingAngle={0}
            isAnimationActive={true}
        >
          {rChartDataValues.map(cd => (
              <Cell
                  key={cd.name}
                  fill={cd.color}
                  stroke="0"
                  filter={cd.filter}
              />
          ))}
        </Pie>
        {/** T相 */}
        <Pie
            data={tChartDataValues}
            dataKey="value"
            cx="50%"
            cy="50%"
            startAngle={-85}
            endAngle={85}
            innerRadius={150}
            outerRadius={190}
            paddingAngle={0}
            isAnimationActive={true}
        >
          {tChartDataValues.map(cd => (
              <Cell
                  key={cd.name}
                  fill={cd.color}
                  stroke="0"
                  filter={cd.filter}
              />
          ))}
        </Pie>
      </BasePieChart>
  );
});

const CenterDisplay = React.memo(({instantaneous}: RendererProps) => {
  const {power, current} = instantaneous;
  return (
      <div className="numeric_meters">
        <div className="numeric_meter">
          <div className="title">Power Now</div>
          <div className="value">
            <NumericFormat
                value={power / 1000}
                displayType="text"
                thousandSeparator={true}
                decimalScale={2}
                allowLeadingZeros={true}
            />
            <span className="unit">kW</span>
          </div>
        </div>
        <hr />
        <div className="numeric_meter">
          <div className="title">Current Now</div>
          <div className="value">
            <NumericFormat
                value={current.sum}
                displayType="text"
                thousandSeparator={true}
            />
            <span className="unit">A</span>
          </div>
        </div>
      </div>
  );
})

export const PowerMonitor = () => {
  const processingLedRef = useRef<ProcessingLedMethods>(null);
  const [isAlarmTriggered, setAlarmTriggered] = useState(false);
  const {data} = useSubscription<{instantaneous: Instantaneous;}>(query, {
    onData() {
      processingLedRef.current?.flush();
    }
  });
  const {data: initialData} = useQuery<{powerSource: PowerSource; instantaneous: Instantaneous;}>(initialQuery);
  const renderData = React.useMemo(() => {
    return {
      powerSource: initialData?.powerSource ?? defaultPowerSource,
      instantaneous: data?.instantaneous ?? initialData?.instantaneous ?? defaultInstantaneous
    } as RendererProps;
  }, [data, initialData]);
  const pieChartData: PieChartRendererProps = {
    width: 400,
    height: 400,
    ...renderData,
  }
  const {powerSource, instantaneous} = renderData;
  const {current} = instantaneous;

  useEffect(() => {
    const {powerSource, instantaneous} = renderData;
    const {current} = instantaneous;
    const alarmCurrentA = Math.max(5, powerSource.ratedCurrentA - 15);
    setAlarmTriggered(current.rPhase > alarmCurrentA || current.tPhase > alarmCurrentA);
  }, [renderData]);

  return (
    <>
      <div className="gauge_container">
        <AlarmSound triggered={isAlarmTriggered} />
        {powerSource.wireCount === 3
            ? <ThreeWirePieChart {...pieChartData} />
            : <TwoWirePieChart {...pieChartData} />}

        <CenterDisplay {...renderData} />

        <ProcessingLed
            ref={processingLedRef}
            style={{
              position: "absolute",
              top: "15px",
              right: "15px"
            }}
        />

        <div className="numeric_meter mini" style={{position: "absolute", bottom: 0, left: 0}}>
          <div className="title">R Phase</div>
          <div className="value">
            <NumericFormat
                value={current.rPhase}
                displayType="text"
                thousandSeparator={true}
            />
            <span className="unit">A</span>
          </div>
        </div>

        <div className="numeric_meter mini" style={{position: "absolute", bottom: 0, right: 0}}>
          <div className="title">T Phase</div>
          <div className="value">
            <NumericFormat
                value={current.tPhase}
                displayType="text"
                thousandSeparator={true}
            />
            <span className="unit">A</span>
          </div>
        </div>
      </div>
    </>
  );
};
