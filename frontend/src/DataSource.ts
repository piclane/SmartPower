import {GraphQLWsLink} from "@apollo/client/link/subscriptions";
import {createClient} from "graphql-ws";
import {ApolloClient, HttpLink, InMemoryCache, split} from "@apollo/client";
import {getMainDefinition} from "@apollo/client/utilities";

const wsLink = new GraphQLWsLink(createClient({
  url: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/graphql`,
}));

const httpLink = new HttpLink({
  uri: `${window.location.protocol}//${window.location.host}/graphql`
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
