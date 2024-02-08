/* eslint max-nested-callbacks: ["error", 6] */

import {getDescriptions} from '@jahia/cypress';

describe('Test for GraphQL schema description', () => {
    // The blacklist is used to ignore nodes that are not registered directly by graphql-core
    // These descriptions should be added in their respective codebases
    const noDescBlacklist = [
        // Missing but provided by: https://github.com/Jahia/npm-modules-engine
        'GqlNpmHelper',

        // // Missing but provided by:  https://github.com/Jahia/content-editor or https://github.com/Jahia/jcontent
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
        'Query/categoryById/id',
        'Query/categoryByPath/path',
        'findAvailableNodeName',
        'MulticriteriaEvaluation',
        'SortType',
        'WipStatus',
        'Mutation/AdminMutation/JahiaAdminMutation/GqlConfigurationMutation',
        'Mutation/JCRMutation',
        'Mutation/WorkflowMutation',
        'Mutation/mutateWorkflows',
        'Subscription/workflowEvent/GqlWorkflowEvent',
        'Subscription/backgroundJobSubscription'
    ];

    const entryNodes = ['Query', 'Mutation', 'Subscription'];
    entryNodes.forEach(entryNode => {
        it(`Description for all nodes under ${entryNode}`, () => {
            getDescriptions(entryNode).then(result => {
                console.log(result);

                // Get the list of nodes that are missing descriptions
                // Remove the nodes that are in the blacklist
                const noDesc = result
                    .filter((graphqlType => graphqlType.description === null || graphqlType.description.length === 0))
                    .filter((graphqlType => !noDescBlacklist.some(t => graphqlType.nodePath.join('/').includes(t))));

                noDesc.forEach(description => {
                    cy.log(`Missing description for ${description.schemaType} at path: ${description.nodePath.join('/')}`);
                    console.log(`Missing description for ${description.schemaType} at path: ${description.nodePath.join('/')}`);
                });
                cy.then(() => expect(noDesc.length).to.equal(0));
            });
        });
    });
});
