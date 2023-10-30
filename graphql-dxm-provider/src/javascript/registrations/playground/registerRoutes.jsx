import {registry} from '@jahia/ui-extender';
import {ApolloSandbox} from '@apollo/sandbox/react';
import React from 'react';

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
export const registerRoutes = () => {
    registry.add('adminRoute', 'graphql-playground', {
        targets: ['developerTools:20'],
        requiredPermission: 'developerToolsAccess',
        icon: window.jahia.moonstone.toIconComponent('GraphQl'),
        label: 'graphql-dxm-provider:graphql',
        isSelectable: true,
        render: () => {
            const url = window.location.origin + window.contextJsParameters.contextPath;
            const subsciptionURL = url.replace(window.location.protocol, window.location.protocol === 'https:' ? 'wss:' : ' ws:');
            return (
                <div style={{height: '100%'}}>
                    <ApolloSandbox
                        initialEndpoint={url + '/modules/graphql'}
                        initialSubscriptionEndpoint={subsciptionURL + '/modules/graphqlws'}
                        initialState={{
                            includeCookies: true,
                            document: initialQuery
                        }}
                    />
                </div>
            );
        }
    });
};
