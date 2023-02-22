import React from 'react';
import './App.css';
import {ApolloProvider} from "@apollo/client";
import {gqlClient} from "@/DataSource";
import {PowerMonitor} from "@/PowerMonitor";

function App() {

  return (
    <div className="App">
      <ApolloProvider client={gqlClient}>
        <PowerMonitor />
      </ApolloProvider>
    </div>
  );
}

export default App;
