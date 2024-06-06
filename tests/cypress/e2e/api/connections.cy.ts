/* eslint-disable quotes */
import {addNode, deleteNode} from '@jahia/cypress';
import gql from 'graphql-tag';
import {DocumentNode} from 'graphql';

describe('GraphQL Connections Test', () => {
    let subNodeCursor1: string;
    let subNodeCursor2: string;
    let subNodeCursor3: string;
    let subNodeCursor4: string;
    let subNodeCursor5: string;
    let result: Cypress.Chainable;

    before('Create test nodes', () => {
        addNode({
            parentPathOrId: '/',
            name: 'testList',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: 'testSubList',
                    primaryNodeType: 'jnt:contentList'
                }
            ]
        })
            .then(() =>
                addNode({
                    parentPathOrId: '/testList/testSubList',
                    name: 'testSubSubList1',
                    primaryNodeType: 'jnt:contentList'
                })
            )
            .then(response => {
                subNodeCursor1 = encodeCursor(response.data.jcr.addNode.uuid);
                return addNode({
                    parentPathOrId: '/testList/testSubList',
                    name: 'testSubSubList2',
                    primaryNodeType: 'jnt:contentList'
                });
            })
            .then(response => {
                subNodeCursor2 = encodeCursor(response.data.jcr.addNode.uuid);
                return addNode({
                    parentPathOrId: '/testList/testSubList',
                    name: 'testSubSubList3',
                    primaryNodeType: 'jnt:contentList'
                });
            })
            .then(response => {
                subNodeCursor3 = encodeCursor(response.data.jcr.addNode.uuid);
                return addNode({
                    parentPathOrId: '/testList/testSubList',
                    name: 'testSubSubList4',
                    primaryNodeType: 'jnt:contentList'
                });
            })
            .then(response => {
                subNodeCursor4 = encodeCursor(response.data.jcr.addNode.uuid);
                return addNode({
                    parentPathOrId: '/testList/testSubList',
                    name: 'testSubSubList5',
                    primaryNodeType: 'jnt:contentList'
                });
            })
            .then(response => {
                subNodeCursor5 = encodeCursor(response.data.jcr.addNode.uuid);
            });
    });
    after('Delete test nodes', () => {
        deleteNode('/testList');
    });

    it('should retrieve nodes using offset and limit', () => {
        result = executeQuery(getQuery(null, null, null, null, 1, 2));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(null, null, null, null, 2, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, null, 2));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        result = executeQuery(getQuery(null, null, null, null, 0, 1));
        insureConnectionResult(result, 5, 1, subNodeCursor1, subNodeCursor1, true, false);

        result = executeQuery(getQuery(null, null, null, null, -15, 1));
        validateError(result, "Argument 'offset' can't be negative");

        result = executeQuery(getQuery(null, null, null, null, 4, 1));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, 4, 5000));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, null, 6, 1));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        result = executeQuery(getQuery(null, null, null, null, 1, -1));
        validateError(result, "Argument 'limit' can't be negative");
    });

    it('should retrieve nodes using cursor', () => {
        result = executeQuery(getQuery(subNodeCursor4, null, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor1, null, null, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, true, false);

        result = executeQuery(getQuery(subNodeCursor5, null, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor1, subNodeCursor4, true, false);

        result = executeQuery(getQuery('wrong_cursor', null, null, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        // Test after
        result = executeQuery(getQuery(null, subNodeCursor2, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor5, null, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        result = executeQuery(getQuery(null, subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor2, subNodeCursor5, false, true);

        result = executeQuery(getQuery('wrong_cursor', null, null, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        // Test first
        result = executeQuery(getQuery(null, null, 3, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(null, null, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor1, subNodeCursor1, true, false);

        result = executeQuery(getQuery(null, null, 40, null, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        result = executeQuery(getQuery(null, null, -1, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        // Test last
        result = executeQuery(getQuery(null, null, null, 3, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, null, null, 40, null, null));
        insureConnectionResult(result, 5, 5, subNodeCursor1, subNodeCursor5, false, false);

        result = executeQuery(getQuery(null, null, null, -1, null, null));
        validateError(result, "Argument 'last' can't be negative");

        // Test before + first
        result = executeQuery(getQuery(subNodeCursor4, null, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, 10, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, -4, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        result = executeQuery(getQuery(subNodeCursor1, null, 2, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, true, false);

        result = executeQuery(getQuery('wrong_cursor', null, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor1, subNodeCursor2, true, false);

        // Test before + last
        result = executeQuery(getQuery(subNodeCursor4, null, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(subNodeCursor4, null, null, 10, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor1, subNodeCursor3, true, false);

        result = executeQuery(getQuery(subNodeCursor4, null, null, -5, null, null));
        validateError(result, "Argument 'last' can't be negative");

        result = executeQuery(getQuery(subNodeCursor1, null, null, 3, null, null));
        insureConnectionResult(result, 5, 0, null, null, true, false);

        result = executeQuery(getQuery('wrong_cursor', null, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        // Test after + first
        result = executeQuery(getQuery(null, subNodeCursor2, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor3, subNodeCursor4, true, true);

        result = executeQuery(getQuery(null, subNodeCursor2, 5, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, -4, null, null, null));
        validateError(result, "Argument 'first' can't be negative");

        result = executeQuery(getQuery(null, subNodeCursor5, 2, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        result = executeQuery(getQuery(null, 'wrong_cursor', 2, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        // Test after + last
        result = executeQuery(getQuery(null, subNodeCursor2, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, null, 4, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor3, subNodeCursor5, false, true);

        result = executeQuery(getQuery(null, subNodeCursor2, null, -3, null, null));
        validateError(result, "Argument 'last' can't be negative");

        result = executeQuery(getQuery(null, subNodeCursor5, null, 2, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        result = executeQuery(getQuery(null, 'wrong_cursor', null, 2, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        // Test after + before
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor4, subNodeCursor4, null, null, null, null));
        // Only the after is apply, regarding relay spec, it's normal because after is applied first
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, null, null, null, null));
        // Only the after is apply, regarding relay spec, it's normal because after is applied first
        insureConnectionResult(result, 5, 2, subNodeCursor4, subNodeCursor5, false, true);

        result = executeQuery(getQuery('wrong_cursor', subNodeCursor1, null, null, null, null));
        insureConnectionResult(result, 5, 4, subNodeCursor2, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor5, 'wrong_cursor', null, null, null, null));
        insureConnectionResult(result, 5, 0, null, null, false, true);

        // Test after + before + last
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, 2, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor3, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, null, 10, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor3, subNodeCursor3, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, null, 1, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor5, subNodeCursor5, false, true);

        // Test after + before + first
        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, 2, null, null, null));
        insureConnectionResult(result, 5, 2, subNodeCursor2, subNodeCursor3, true, true);

        result = executeQuery(getQuery(subNodeCursor5, subNodeCursor1, 10, null, null, null));
        insureConnectionResult(result, 5, 3, subNodeCursor2, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor3, subNodeCursor3, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor4, subNodeCursor4, true, true);

        result = executeQuery(getQuery(subNodeCursor2, subNodeCursor3, 1, null, null, null));
        insureConnectionResult(result, 5, 1, subNodeCursor4, subNodeCursor4, true, true);
    });

    it('should return an error when using offset limit and cursor pagination', () => {
        const cursorPossibilities = getArrayOfPossibilitiesWithAtLeastOneTrue(4);
        const offsetLimitPossibilities = getArrayOfPossibilitiesWithAtLeastOneTrue(2);

        offsetLimitPossibilities.forEach(offsetLimitPossibility => {
            cursorPossibilities.forEach(cursorPossibility => {
                const before = cursorPossibility[0] ? subNodeCursor5 : null;
                const after = cursorPossibility[1] ? subNodeCursor1 : null;
                const first = cursorPossibility[2] ? 2 : null;
                const last = cursorPossibility[3] ? 2 : null;
                const offset = offsetLimitPossibility[0] ? 2 : null;
                const limit = offsetLimitPossibility[1] ? 2 : null;
                result = executeQuery(getQuery(before, after, first, last, offset, limit));
                validateError(result, "Offset and/or Limit argument(s) can't be used with other pagination arguments");
            });
        });
    });
});

const encodeCursor = (cursor: string): string => {
    return Buffer.from(cursor).toString('base64');
};

const executeQuery = (query: DocumentNode): Cypress.Chainable => {
    return cy.apollo({
        query
    });
};

const getQuery = (
    before: string | null,
    after: string | null,
    first: number | null,
    last: number | null,
    offset: number | null,
    limit: number | null
    // eslint-disable-next-line max-params
): DocumentNode => {
    return gql`
        query {
            jcr {
                nodesByQuery(
                    query: "Select * from [jnt:contentList] as cl where isdescendantnode(cl, ['/testList/testSubList']) order by cl.[j:nodename]",
                    before: ${before === null ? null : `"${before}"`},
                    after: ${after === null ? null : `"${after}"`},
                    first: ${first},
                    last: ${last},
                    offset: ${offset},
                    limit: ${limit}
                ) {
                    edges {
                        index,
                        cursor,
                        node {
                            name,
                            uuid
                        }
                    },
                    pageInfo {
                        totalCount,
                        nodesCount,
                        startCursor,
                        endCursor,
                        hasNextPage,
                        hasPreviousPage
                    },
                    nodes {
                        name,
                        uuid
                    }
                }
            }
        }
    `;
};

const insureConnectionResult = (
    result: Cypress.Chainable,
    expectedTotalCount: number,
    expectedNodesCount: number,
    expectedStartCursor: string | null,
    expectedEndCursor: string | null,
    expectedHasNextPage: boolean,
    expectedHasPreviousPage: boolean
    // eslint-disable-next-line max-params
) => {
    result.should(response => {
        const pageInfo = response.data.jcr.nodesByQuery.pageInfo;
        expect(pageInfo.totalCount).to.equal(expectedTotalCount);
        expect(pageInfo.nodesCount).to.equal(expectedNodesCount);

        if (expectedStartCursor) {
            expect(pageInfo.startCursor).to.equal(expectedStartCursor);
        } else {
            expect(pageInfo.startCursor).to.be.null;
        }

        if (expectedEndCursor) {
            expect(pageInfo.endCursor).to.equal(expectedEndCursor);
        } else {
            expect(pageInfo.endCursor).to.be.null;
        }

        expect(pageInfo.hasNextPage).to.equal(expectedHasNextPage);
        expect(pageInfo.hasPreviousPage).to.equal(expectedHasPreviousPage);
    });
};

const validateError = (result: Cypress.Chainable, expectedError: string) => {
    result.should(response => {
        const errors = response?.graphQLErrors;
        expect(errors).to.exist;
        expect(errors).to.have.length(1);

        expect(errors[0]?.message).to.contain(expectedError);
    });
};

const getArrayOfPossibilitiesWithAtLeastOneTrue = (n: number): Array<Array<boolean>> => {
    const possibilities = [];

    for (let i = 0; i < Math.pow(2, n); i++) {
        const bin = i.toString(2).padStart(n, '0');
        const boolArray = Array.from(bin, x => x === '0');

        if (boolArray.includes(true)) {
            possibilities.push(boolArray);
        }
    }

    return possibilities;
};
