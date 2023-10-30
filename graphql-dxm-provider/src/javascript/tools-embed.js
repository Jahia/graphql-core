import {ApolloSandbox} from '@apollo/sandbox';

const initialQuery = `
query {
    admin {
        jahia {
            version {
                release
            }
        }
    }
}`;
export const EmbeddedSandbox = target => {
    const url = window.location.origin + window.contextJsParameters.contextPath;
    const subsciptionURL = url.replace(window.location.protocol, window.location.protocol === 'https:' ? 'wss:' : ' ws:');
    // eslint-disable-next-line
    new ApolloSandbox({
        target: target,
        initialEndpoint: url + '/modules/graphql',
        initialSubscriptionEndpoint: subsciptionURL + '/modules/graphqlws',
        initialState: {
            includeCookies: true,
            document: initialQuery
        }
    });
};
