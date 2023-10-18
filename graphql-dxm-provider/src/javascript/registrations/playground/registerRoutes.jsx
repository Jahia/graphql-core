import {registry} from '@jahia/ui-extender';
import {ApolloSandbox} from '@apollo/sandbox/react';
import React from 'react';

export const registerRoutes = () => {
    registry.add('adminRoute', 'graphql-playground', {
        targets: ['developerTools:20'],
        requiredPermission: 'developerToolsAccess',
        icon: window.jahia.moonstone.toIconComponent('GraphQl'),
        label: 'graphql-dxm-provider:graphql',
        isSelectable: true,
        render: () => (
            <div style={{height: '100%'}}>
                <ApolloSandbox
                    initialEndpoint="http://localhost:8080/modules/graphql"
                    endpointIsEditable={false}
                    initialSubscriptionEndpoint="ws://localhost:8080/modules/graphqlws"
                    initialState={{
                        includeCookies: true,
                        document: `
query {
	admin {
		jahia {
			version {
				release
			}
		}
	}
}
`
                    }}
                />
            </div>
        )
    });
};
