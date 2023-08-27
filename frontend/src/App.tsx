import React from 'react';
import './App.css';
import {ApolloProvider} from "@apollo/client";
import {gqlClient} from "@/DataSource";
import {PowerMonitor} from "@/PowerMonitor";
import {DigitalClock} from "@/DigitalClock";

function App() {

  return (
    <div className="App">
      <ApolloProvider client={gqlClient}>
        <PowerMonitor />
        <DigitalClock
            style={{
              position: "fixed",
              left: "1rem",
              bottom: "1rem",
              fontSize: "9vh",
            }}
        />
      </ApolloProvider>
    </div>
  );
}

export default App;
