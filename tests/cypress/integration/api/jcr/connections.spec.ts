/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'

interface Run {
    t: string
    q: {
        beforeIdx?: number
        afterIdx?: number
        first?: number
        last?: number
        offset?: number
        limit?: number
    }
    r?: {
        totalCount: number
        nodesCount: number
        startCursorIdx: number
        endCursorIdx: number
        hasNextPage: boolean
        hasPreviousPage: boolean
    }
    error?: string
}

describe('GraphQLConnectionsTest', () => {
    let GQL_ADDNODE: DocumentNode
    let GQL_DELETENODE: DocumentNode
    let GQL_CONNECTIONS: DocumentNode
    const testSubSubList: Array<string> = ['this-is-not-a-uuid-and-it-does-not-exist']
    const baseQuery =
        "Select * from [jnt:contentList] as cl where isdescendantnode(cl, ['/testList/testSubList']) order by cl.[j:nodename]"

    before('load graphql file and create test dataset', () => {
        GQL_ADDNODE = require(`graphql-tag/loader!../../../fixtures/jcr/addNode.graphql`)
        GQL_DELETENODE = require(`graphql-tag/loader!../../../fixtures/jcr/deleteNode.graphql`)
        GQL_CONNECTIONS = require(`graphql-tag/loader!../../../fixtures/jcr/connections.graphql`)

        cy.log('Preparing the test suite dataset: createList')
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            mode: 'mutate',
            variables: {
                parentPathOrId: '/',
                nodeName: 'testList',
                nodeType: 'jnt:contentList',
            },
            query: GQL_ADDNODE,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.addNode.uuid).not.to.be.null
            cy.task('apolloNode', {
                baseUrl: Cypress.config().baseUrl,
                authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
                mode: 'mutate',
                variables: {
                    parentPathOrId: '/testList',
                    nodeName: 'testSubList',
                    nodeType: 'jnt:contentList',
                },
                query: GQL_ADDNODE,
            }).then((response: any) => {
                cy.log(JSON.stringify(response))
                expect(response.data.jcr.addNode.uuid).not.to.be.null
                for (const x of [1, 2, 3, 4, 5]) {
                    cy.task('apolloNode', {
                        baseUrl: Cypress.config().baseUrl,
                        authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
                        mode: 'mutate',
                        variables: {
                            parentPathOrId: '/testList/testSubList',
                            nodeName: `testSubSubList${x}`,
                            nodeType: 'jnt:contentList',
                        },
                        query: GQL_ADDNODE,
                    }).then((response: any) => {
                        cy.log(JSON.stringify(response))
                        cy.log(`testSubSubList${x}: ${response.data.jcr.addNode.uuid}`)
                        expect(response.data.jcr.addNode.uuid).not.to.be.null
                        testSubSubList.push(response.data.jcr.addNode.uuid)
                    })
                }
            })
        })
    })

    after('Clear the created dataset', () => {
        cy.log('Preparing the test suite dataset: createList')
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            mode: 'mutate',
            variables: {
                pathOrId: '/testList',
            },
            query: GQL_DELETENODE,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.deleteNode).to.be.true
        })
    })

    const runsDataset: Array<Run> = [
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 1, limit: 2',
            q: { offset: 1, limit: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 2,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 2, limit: null',
            q: { offset: 2, limit: null },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 3,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: null, limit: 2',
            q: { offset: null, limit: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 1,
                endCursorIdx: 2,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 0, limit: 1',
            q: { offset: 0, limit: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 1,
                endCursorIdx: 1,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: -15, limit: 1 (testing for error)',
            q: { offset: -15, limit: 1 },
            error: "Argument 'offset' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 4, limit: 1',
            q: { offset: 4, limit: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 4, limit: 5000',
            q: { offset: 4, limit: 5000 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 6, limit: 1',
            q: { offset: 6, limit: 1 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingOffsetAndLimit - Offset: 1, limit: -1 (testing for error)',
            q: { offset: 1, limit: -1 },
            error: "Argument 'limit' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4',
            q: { beforeIdx: 4 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 1,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList1',
            q: { beforeIdx: 1 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5',
            q: { beforeIdx: 5 },
            r: {
                totalCount: 5,
                nodesCount: 4,
                startCursorIdx: 1,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before INVALID cursor',
            q: { beforeIdx: 0 },
            r: {
                totalCount: 5,
                nodesCount: 5,
                startCursorIdx: 1,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2',
            q: { afterIdx: 2 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 3,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList5',
            q: { afterIdx: 5 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList1',
            q: { afterIdx: 1 },
            r: {
                totalCount: 5,
                nodesCount: 4,
                startCursorIdx: 2,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After INVALID cursor',
            q: { afterIdx: 0 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - First 3 nodes',
            q: { first: 3 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 1,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - First 1 nodes',
            q: { first: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 1,
                endCursorIdx: 1,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - First 40 nodes',
            q: { first: 40 },
            r: {
                totalCount: 5,
                nodesCount: 5,
                startCursorIdx: 1,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - First -1 nodes (testing for error)',
            q: { first: -1 },
            error: "Argument 'first' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Last 3 nodes',
            q: { last: 3 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 3,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Last 1 nodes',
            q: { last: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Last 40 nodes',
            q: { last: 40 },
            r: {
                totalCount: 5,
                nodesCount: 5,
                startCursorIdx: 1,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Last -1 nodes (testing for error)',
            q: { last: -1 },
            error: "Argument 'last' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and first 2',
            q: { beforeIdx: 4, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 1,
                endCursorIdx: 2,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and first 10',
            q: { beforeIdx: 4, first: 10 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 1,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and first -4 (testing for error)',
            q: { beforeIdx: 4, first: -4 },
            error: "Argument 'first' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList1 and first 2',
            q: { beforeIdx: 1, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before INVALID cursor and first 2',
            q: { beforeIdx: 0, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 1,
                endCursorIdx: 2,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and last 2',
            q: { beforeIdx: 4, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 2,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and last 10',
            q: { beforeIdx: 4, last: 10 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 1,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and last -5 (testing for error)',
            q: { beforeIdx: 4, last: -5 },
            error: "Argument 'last' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList1 and last 3',
            q: { beforeIdx: 1, last: 3 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: true,
                hasPreviousPage: false,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before INVALID cursor and last 2',
            q: { beforeIdx: 0, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 4,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and first 2',
            q: { afterIdx: 2, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 3,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and first 5',
            q: { afterIdx: 2, first: 5 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 3,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and first -4 (testing for error)',
            q: { afterIdx: 2, first: -4 },
            error: "Argument 'first' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList5 and first 2',
            q: { afterIdx: 5, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After INVALID cursor and first 2',
            q: { afterIdx: 0, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and last 2',
            q: { afterIdx: 2, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 4,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and last 4',
            q: { afterIdx: 2, last: 4 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 3,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList2 and last -3',
            q: { afterIdx: 2, last: -3 },
            error: "Argument 'last' can't be negative",
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After cursor of testSubSubList5 and last 4',
            q: { afterIdx: 5, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - After INVALID cursor and last 2',
            q: { afterIdx: 0, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5 and after cursor of testSubSubList1',
            q: { beforeIdx: 5, afterIdx: 1 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 2,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList4 and after cursor of testSubSubList4',
            // Note from previous test framework:
            // only the after is apply, regarding relay spec, it's normal because after is applied first
            q: { beforeIdx: 4, afterIdx: 4 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList2 and after cursor of testSubSubList3',
            // Note from previous test framework:
            // only the after is apply, regarding relay spec, it's normal because after is applied first
            q: { beforeIdx: 2, afterIdx: 3 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 4,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before INVALID cursor and after cursor of testSubSubList1',
            q: { beforeIdx: 0, afterIdx: 1 },
            r: {
                totalCount: 5,
                nodesCount: 4,
                startCursorIdx: 2,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t: 'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5 and after INVALID cursor',
            q: { beforeIdx: 5, afterIdx: 0 },
            r: {
                totalCount: 5,
                nodesCount: 0,
                startCursorIdx: null,
                endCursorIdx: null,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5, after cursor of testSubSubList1 and last 2',
            q: { beforeIdx: 5, afterIdx: 1, last: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 3,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5, after cursor of testSubSubList1 and last 10',
            q: { beforeIdx: 5, afterIdx: 1, last: 10 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 2,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList3, after cursor of testSubSubList3 and last 1',
            q: { beforeIdx: 3, afterIdx: 3, last: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList2, after cursor of testSubSubList3 and last 1',
            q: { beforeIdx: 2, afterIdx: 3, last: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 5,
                endCursorIdx: 5,
                hasNextPage: false,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList2, after cursor of testSubSubList3 and first 2',
            q: { beforeIdx: 5, afterIdx: 1, first: 2 },
            r: {
                totalCount: 5,
                nodesCount: 2,
                startCursorIdx: 2,
                endCursorIdx: 3,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList5, after cursor of testSubSubList1 and first 10',
            q: { beforeIdx: 5, afterIdx: 1, first: 10 },
            r: {
                totalCount: 5,
                nodesCount: 3,
                startCursorIdx: 2,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList3, after cursor of testSubSubList3 and first 1',
            q: { beforeIdx: 3, afterIdx: 3, first: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 4,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldRetrieveNodesUsingCursor - Before cursor of testSubSubList2, after cursor of testSubSubList3 and first 1',
            q: { beforeIdx: 2, afterIdx: 3, first: 1 },
            r: {
                totalCount: 5,
                nodesCount: 1,
                startCursorIdx: 4,
                endCursorIdx: 4,
                hasNextPage: true,
                hasPreviousPage: true,
            },
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both before and offset',
            q: { beforeIdx: 2, offset: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both before and limit',
            q: { beforeIdx: 2, limit: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both after and offset',
            q: { afterIdx: 2, offset: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both after and limit',
            q: { afterIdx: 2, limit: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both first and offset',
            q: { first: 2, offset: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both first and limit',
            q: { first: 2, limit: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both last and offset',
            q: { last: 2, offset: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
        {
            t:
                'shouldReturnAnErrorWhenUsingOffsetLimitAndCursorPagination - Validate not possible to use both last and limit',
            q: { last: 2, limit: 1 },
            error: "Offset and/or Limit argument(s) can't be used with other pagination arguments",
        },
    ]
    for (const run of runsDataset) {
        it(run.t, () => {
            cy.task('apolloNode', {
                baseUrl: Cypress.config().baseUrl,
                authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
                variables: {
                    query: baseQuery,
                    before: run.q.beforeIdx === undefined ? null : btoa(testSubSubList[run.q.beforeIdx]),
                    after: run.q.afterIdx === undefined ? null : btoa(testSubSubList[run.q.afterIdx]),
                    first: run.q.first === undefined ? null : run.q.first,
                    last: run.q.last === undefined ? null : run.q.last,
                    offset: run.q.offset === undefined ? null : run.q.offset,
                    limit: run.q.limit === undefined ? null : run.q.limit,
                },
                query: GQL_CONNECTIONS,
            }).then((response: any) => {
                cy.log(JSON.stringify(response))
                if (run.error !== undefined) {
                    expect(response.errors[0].message).to.equal(run.error)
                } else {
                    expect(response.data.jcr.nodesByQuery.pageInfo.totalCount).to.equal(run.r.totalCount)
                    expect(response.data.jcr.nodesByQuery.pageInfo.nodesCount).to.equal(run.r.nodesCount)
                    if (run.r.startCursorIdx !== null) {
                        expect(atob(response.data.jcr.nodesByQuery.pageInfo.startCursor)).to.equal(
                            testSubSubList[run.r.startCursorIdx],
                        )
                    } else {
                        expect(response.data.jcr.nodesByQuery.pageInfo.startCursor).to.be.null
                    }
                    if (run.r.endCursorIdx !== null) {
                        expect(atob(response.data.jcr.nodesByQuery.pageInfo.endCursor)).to.equal(
                            testSubSubList[run.r.endCursorIdx],
                        )
                    } else {
                        expect(response.data.jcr.nodesByQuery.pageInfo.endCursor).to.be.null
                    }
                    expect(response.data.jcr.nodesByQuery.pageInfo.hasNextPage).to.equal(run.r.hasNextPage)
                    expect(response.data.jcr.nodesByQuery.pageInfo.hasPreviousPage).to.equal(run.r.hasPreviousPage)
                }
            })
        })
    }
})
