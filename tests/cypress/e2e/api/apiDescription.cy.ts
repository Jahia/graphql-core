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
        'AdminQuery/jahia/JahiaAdminQuery/healthCheck',
        'AdminQuery/jahia/JahiaAdminQuery/GqlHealthCheck',
        'AdminQuery/jahia/JahiaAdminQuery/load',

        // Missing but provided by: https://github.com/Jahia/personal-api-tokens
        'AdminQuery/personalApiTokens/PersonalApiTokensQuery',

        // Missing but provided by: https://github.com/Jahia/security-filter-tools
        'JWTToken',
        'jwtToken',

        // These are provided by graphql-dxm-provider
        // Descriptions should be added via a separate ticket: BACKLOG-22338
        'findAvailableNodeName',
        'categoryById/id',
        'categoryByPath/path',
        'InputFieldFiltersInput/multi/MulticriteriaEvaluation',
        'InputFieldSorterInput/sortType/SortType',
        'InputFieldGroupingInput/groupingType/GroupingType',
        'JCRProperty/type/JCRPropertyType',
        'GqlPublicationInfo/publicationStatus/PublicationStatus',
        'wipInfo/status/WipStatus',
        'GqlBackgroundJob/jobState/GqlBackgroundJobState',
        'GqlBackgroundJob/jobStatus/GqlBackgroundJobStatus',

        'Mutation/mutateWorkflows',
        'configuration/GqlConfigurationMutation',
        'AdminMutation/personalApiTokens',
        'Mutation/jcr/JCRMutation',

        'Subscription/workflowEvent/GqlWorkflowEvent'
    ];

    // Note: If you need to remove a node missing a description, please use the noDescBlacklist
    // array, do not filter out one of the entryNodes entirely as it will make the test blind
    // to any further updates to the schema.
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

                // Get the list of nodes that are deprecated and ensure an explanation is present (deprecationReason)*
                // "Deprecated" (in all its forms) is not considered a valid deprecationReason
                // Remove the nodes that are in the blacklist
                const noDeprecateReason = result
                    .filter((graphqlType => (graphqlType.isDeprecated === true && (!graphqlType.deprecationReason || graphqlType.deprecationReason.length === 0 || (graphqlType.deprecationReason instanceof String && graphqlType.deprecationReason.toLowercase() === ('Deprecated').toLowerCase())))))
                    .filter((graphqlType => !noDescBlacklist.some(t => graphqlType.nodePath.join('/').includes(t))));

                noDeprecateReason.forEach(description => {
                    cy.log(`Deprecated ${description.schemaType} missing explanation at path: ${description.nodePath.join('/')}`);
                    console.log(`Deprecated ${description.schemaType} missing explanation at path: ${description.nodePath.join('/')}`);
                });
                cy.then(() => expect(noDesc.length).to.equal(0));
            });
        });
    });
});
