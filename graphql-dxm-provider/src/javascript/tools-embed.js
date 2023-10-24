import {ApolloSandbox} from '@apollo/sandbox';

export const EmbeddedSandbox = target => {
    // eslint-disable-next-line
    new ApolloSandbox({
        target: target,
        initialEndpoint: 'http://localhost:8080/modules/graphql',
        endpointIsEditable: true,
        initialSubscriptionEndpoint: 'ws://localhost:8080/modules/graphqlws',
        initialState: {
            includeCookies: true,
            document: 'query Version {\n' +
                '\tadmin {\n' +
                '\t\tjahia {\n' +
                '\t\t\tversion {\n' +
                '\t\t\t\trelease\n' +
                '\t\t\t}\n' +
                '\t\t}\n' +
                '\t}\n' +
                '}'
        }
    });
};
