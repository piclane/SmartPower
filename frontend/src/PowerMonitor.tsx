import gql from "graphql-tag";
import {useSubscription} from "@apollo/client";
import React from "react";
import {amber, blue, deepOrange, orange} from 'material-ui-colors'
import {Cell, Label, Pie, PieChart} from "recharts";
import './PowerMonitor.css';
import {NumericFormat} from "react-number-format";

const query = gql`
    subscription SmartPower {
        instantaneous {
            power
            current {
                sum
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

export const PowerMonitor = () => {
  const { data } = useSubscription(
      query
  );

  const instantaneous = data?.instantaneous;
  const power = instantaneous?.power ?? 0;
  const current = instantaneous?.current?.sum ?? 0;
  const maxCurrent = 60;
  const chartDataValues = [
    {name: 'used', value: current, color: buildColor(current / maxCurrent)},
    {name: 'free', value: maxCurrent - current, color: '#edebeb', filter: 'url(#filter_0)'},
  ];

  return (
    <>
      <div className="gauge_container">
        <PieChart width={400} height={400}>
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
          <Pie
              data={chartDataValues}
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
            {chartDataValues.map(cd => (
                <Cell
                    key={cd.name}
                    fill={cd.color}
                    stroke="0"
                    filter={cd.filter}
                />
            ))}
          </Pie>
        </PieChart>
        <div className="numeric_meters">
          <div className="numeric_meter">
            <div className="title">Power Now</div>
            <div className="value">
              <NumericFormat value={power / 1000} displayType="text" thousandSeparator={true} decimalScale={2} />
              <span className="unit">kW</span>
            </div>
          </div>
          <hr />
          <div className="numeric_meter">
            <div className="title">Current Now</div>
            <div className="value">
              <NumericFormat value={current} displayType="text" thousandSeparator={true} />
              <span className="unit">A</span>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
