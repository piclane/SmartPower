import {GraphQLWsLink} from "@apollo/client/link/subscriptions";
import {createClient} from "graphql-ws";
import {ApolloClient, HttpLink, InMemoryCache, split} from "@apollo/client";
import {getMainDefinition} from "@apollo/client/utilities";

let activeSocket: any = null;
let timedOut: any = null;

const protocol = window.location.protocol;
const host = window.location.host;
const wsProtocol = protocol === 'https:' ? 'wss:' : 'ws:';
const wsUrl = `${wsProtocol}//${host}/graphql`;
const httpUrl = `${protocol}//${host}/graphql`;

const wsLink = new GraphQLWsLink(createClient({
  url: wsUrl,
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
  uri: httpUrl
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
