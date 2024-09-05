import {registry} from '@jahia/ui-extender';
import React, {useEffect} from 'react';
import {explorerPlugin} from '@graphiql/plugin-explorer';
import 'graphiql/graphiql.css';
import '@graphiql/plugin-explorer/dist/style.css';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import {GraphiQL} from 'graphiql';
import {SubscriptionClient} from 'subscriptions-transport-ws';
import {useTheme} from '@graphiql/react';

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

/**
 * Instantiate outside the component lifecycle
 * unless you need to pass it dynamic values from your react app,
 * then use the `useMemo` hook
 */
const explorer = explorerPlugin();
const GraphiQLComponent = () => {
    const {setTheme} = useTheme();
    const url = window.location.origin + window.contextJsParameters.contextPath;
    const subscriptionURL = url.replace(window.location.protocol, window.location.protocol === 'https:' ? 'wss:' : ' ws:');
    const fetcher = React.useMemo(
        () => createGraphiQLFetcher({
            url: url + '/modules/graphql',
            legacyWsClient: new SubscriptionClient(subscriptionURL + '/modules/graphqlws')
        }),
        [url, subscriptionURL]
    );
    useEffect(() => {
        setTheme('dark');
    }, [setTheme]);
    return (
        <div style={{height: '100%'}}>
            <GraphiQL
                plugins={[explorer]}
                fetcher={fetcher}
                defaultQuery={initialQuery}
            />
        </div>
    );
};

export const registerRoutes = () => {
    if (window?.contextJsParameters?.config?.graphqlDxmProvider?.isIntrospectionEnabled) {
        registry.add('adminRoute', 'graphql-workspace', {
            targets: ['developerTools:20'],
            requiredPermission: 'developerToolsAccess',
            icon: window.jahia.moonstone.toIconComponent('GraphQl'),
            label: 'graphql-dxm-provider:graphql',
            isSelectable: true,
            render: () => <GraphiQLComponent/>
        });
    }
};
