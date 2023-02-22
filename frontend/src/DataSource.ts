import {GraphQLWsLink} from "@apollo/client/link/subscriptions";
import {createClient} from "graphql-ws";
import {ApolloClient, HttpLink, InMemoryCache, split} from "@apollo/client";
import {getMainDefinition} from "@apollo/client/utilities";

const wsLink = new GraphQLWsLink(createClient({
  url: `ws://${window.location.host}/graphql`,
}));

const httpLink = new HttpLink({
  uri: `http://${window.location.host}/graphql`
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
