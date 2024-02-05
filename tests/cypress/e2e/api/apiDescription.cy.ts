/* eslint max-nested-callbacks: ["error", 6] */

import {getDescriptions} from '@jahia/cypress';

describe('Test if every all nodes of the GraphQL schema have a description', () => {
    // The blacklist is used to ignore nodes that are not registered directly by graphql-core
    // These descriptions should be added in their respective codebases
    const noDescBlacklist = [
        // Missing but provided by: https://github.com/Jahia/npm-modules-engine
        'GqlNpmHelper',

        // Missing but provided by:  https://github.com/Jahia/content-editor or https://github.com/Jahia/jcontent
        'GqlEditorForms',
        'GqlEditorForm',

        // Missing but provided by: https://github.com/Jahia/jahia-dashboard
        'GqlDashboard',

        // Missing but provided by: https://github.com/Jahia/server-availability-manager
        'AdminQuery/JahiaAdminQuery/healthCheck',
        'AdminQuery/JahiaAdminQuery/GqlHealthCheck',
        'AdminQuery/JahiaAdminQuery/Load',

        // Missing but provided by: https://github.com/Jahia/security-filter-tools
        'JWTToken',
        'jwtToken',

        // These are provided by graphql-dxm-provider
        // Descriptions should be added via a separate ticket:
        'Query/WorkflowService',
        'Mutation/AdminMutation/JahiaAdminMutation/GqlConfigurationMutation',
        'Mutation/JCRMutation',
        'Mutation/WorkflowMutation',
        'Mutation/mutateWorkflows',
        'Subscription/GqlWorkflowEvent'
    ];

    const entryNodes = ['Query', 'Mutation', 'Subscription'];
    entryNodes.forEach(entryNode => {
        it(`Verify presence of a description all nodes under ${entryNode}`, () => {
            getDescriptions(entryNode).then(result => {
                console.log(result);

                // Get the list of nodes that are missing descriptions
                // Remove the nodes that are in the blacklist
                const noDesc = result
                    .filter((graphqlType => graphqlType.description === null || graphqlType.description.length === 0))
                    .filter((graphqlType => !noDescBlacklist.some(t => graphqlType.nodePath.join('/').includes(t))));

                noDesc.forEach(graphqlType => {
                    cy.log('Missing description for node at path: ' + graphqlType.nodePath.join('/'));
                    console.log('Missing description for type: ' + graphqlType.name + ' in path: ' + graphqlType.nodePath.join('/'), graphqlType);
                });
                cy.then(() => expect(noDesc.length).to.equal(0));
            });
        });
    });
});
