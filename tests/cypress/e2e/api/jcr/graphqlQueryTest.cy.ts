/* eslint-disable @typescript-eslint/no-explicit-any */
import gql from 'graphql-tag';
import {validateError} from './validateErrors';

describe('GraphQL Query Test', () => {
    /* Setup */

    before('Create query test nodes', () => {
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/',
                nodeName: 'testList',
                nodeType: 'jnt:contentList',
                children: [
                    {name: 'testSubList1', primaryNodeType: 'jnt:contentList'},
                    {name: 'testSubList2', primaryNodeType: 'jnt:contentList'},
                    {name: 'testSubList3', primaryNodeType: 'jnt:contentList'},
                    {
                        name: 'testSubList4',
                        primaryNodeType: 'jnt:contentList',
                        children: [
                            {name: 'testSubList4_1', primaryNodeType: 'jnt:contentList'},
                            {name: 'testSubList4_2', primaryNodeType: 'jnt:contentList'},
                            {name: 'testSubList4_3', primaryNodeType: 'jnt:contentList'}
                        ]
                    }
                ]
            }
        });
    });

    after('Remove query test nodes', () => {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {pathOrId: '/testList'}
        });
    });

    /* Tests */

    it('Should retrieve nodes by SQL2 query', () => {
        const query = 'select * from [jnt:contentList] where isdescendantnode(\'/testList\')';
        const queryLang = 'SQL2';
        testQuery(query, queryLang, 7);
    });

    it('Should retrieve nodes by Xpath query', () => {
        const query = '/jcr:root/testList//element(*, jnt:contentList)';
        const queryLang = 'XPATH';
        testQuery(query, queryLang, 7);
    });

    it('Should get error not retrieve nodes by wrong query', () => {
        const query = 'slct from [jnt:contentList]';
        const queryLang = 'SQL2';
        runQuery(query, queryLang, 'all').should((result: any) => {
            validateError(
                result,
                'javax.jcr.query.InvalidQueryException: Query:\nslct(*)from [jnt:contentList]; expected: SELECT'
            );
        });
    });

    /* Helper methods */

    function testQuery(query, queryLang, expectedNodeLength) {
        runQuery(query, queryLang).should((result: any) => {
            const nodes = result?.data?.jcr?.nodesByQuery?.edges;
            expect(nodes.length).to.equal(expectedNodeLength);
        });
    }

    function runQuery(query, queryLanguage, errorPolicy = undefined) {
        return cy.apollo({
            query: gql`
                query ($query: String!, $queryLanguage: QueryLanguage!) {
                    jcr {
                        nodesByQuery(query: $query, queryLanguage: $queryLanguage) {
                            edges {
                                node {
                                    path
                                }
                            }
                        }
                    }
                }
            `,
            variables: {query, queryLanguage},
            errorPolicy
        });
    }
});
