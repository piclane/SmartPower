import {GraphQLWsLink} from "@apollo/client/link/subscriptions";
import {createClient} from "graphql-ws";
import {ApolloClient, HttpLink, InMemoryCache, split} from "@apollo/client";
import {getMainDefinition} from "@apollo/client/utilities";

// const wsLink = new GraphQLWsLink(createClient({
//   url: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/graphql`,
// }));
//
// const httpLink = new HttpLink({
//   uri: `${window.location.protocol}//${window.location.host}/graphql`
// });

let activeSocket: any = null;
let timedOut: any = null;

const wsLink = new GraphQLWsLink(createClient({
  url: `wss://power-monitor.piclane.xxuz.com/graphql`,
  retryAttempts: Infinity,
  shouldRetry: () => true,
  on: {
    connected: socket => {
      activeSocket = socket
    },
    ping: (received) => {
      if (!received)
        // sent
        timedOut = setTimeout(() => {
          if (activeSocket?.readyState === WebSocket?.OPEN)
            activeSocket?.close(4408, 'Request Timeout');
        }, 5000); // wait 5 seconds for the pong and then close the connection
    },
    pong: (received) => {
      if (received) clearTimeout(timedOut); // pong is received, clear connection close timeout
    }
  }
}));
const httpLink = new HttpLink({
  uri: `https://power-monitor.piclane.xxuz.com/graphql`
});

const splitLink = split(
    ({ query }) => {
      const definition = getMainDefinition(query);
      return (
          definition.kind === 'OperationDefinition' &&
          definition.operation === 'subscription'
      );
    },
    wsLink,
    httpLink,
);

export const gqlClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
});
