import gql from 'graphql-tag';

/*
 * Per-request node allowance (graphql.fields.node.requestLimit).
 *
 * graphql.fields.node.limit caps how many nodes ONE connection collects. It says nothing about how many connections a
 * request may open, and a nested selection opens one inner connection per node the outer one returned - so the work a
 * request does is the product of those caps, not the largest of them. A document nesting `descendants` is textually
 * small and shallow, passes maxComplexity and maxDepth comfortably, and still stands for an unbounded walk; a few in
 * parallel are enough to exhaust a server's heap. This bound is what closes that: the allowance is shared by every
 * field of the request and spent as nodes are actually read.
 *
 * The tests below are built around a fixture subtree of 155 nodes (three levels of five). Walking it once reads 155
 * nodes; walking it with `descendants` nested one level deeper reads 430. With the allowance set to 200 the first is
 * served and the second refused - so a pass and a failure here differ only in nesting, which is precisely the dimension
 * the per-connection limit cannot see: every connection in either query is far below graphql.fields.node.limit.
 *
 * Selecting pageInfo.totalCount reads every node the connection matches regardless of the page size, and those reads
 * are charged like any other. The totalCount tests pin that down with the allowance at 100: a one-item page over the
 * 155-node subtree passes without totalCount and is refused with it.
 *
 * Only honoured from the default provider configuration, so the limit is driven through a groovy provisioning fixture
 * that edits the "default" factory instance (see setRequestNodeLimit.groovy). Config propagation (ConfigAdmin update
 * -> ManagedServiceFactory.updated) is asynchronous, so we poll until the new value takes effect rather than waiting a
 * fixed delay.
 */
describe('GraphQL per-request node allowance', () => {
    const waitOptions = {interval: 500, timeout: 30000};

    // The shipped default, restored after the suite so later specs are unaffected.
    const SHIPPED_REQUEST_NODE_LIMIT = 20000;
    const TEST_ROOT = '/sites/systemsite/contents/requestNodeLimitTest';

    // Walks the fixture subtree once: 155 nodes, comfortably inside an allowance of 200.
    const oneLevelQuery = gql`
        query {
            jcr {
                nodeByPath(path: "${TEST_ROOT}") {
                    descendants {
                        nodes { name }
                    }
                }
            }
        }
    `;

    // A one-item page over the same subtree, but selecting totalCount: computing the total reads every node the
    // connection matches, however small the page, and those reads are charged like any other. With the allowance
    // below the subtree size this is refused - the page alone would cost 2 nodes.
    const totalCountQuery = gql`
        query {
            jcr {
                nodeByPath(path: "${TEST_ROOT}") {
                    descendants(first: 1) {
                        pageInfo { totalCount }
                        nodes { name }
                    }
                }
            }
        }
    `;

    // The same one-item page without totalCount, which only the page itself is charged for.
    const firstPageOnlyQuery = gql`
        query {
            jcr {
                nodeByPath(path: "${TEST_ROOT}") {
                    descendants(first: 1) {
                        nodes { name }
                    }
                }
            }
        }
    `;

    // The same subtree, one level of nesting deeper. Every individual connection stays far below
    // graphql.fields.node.limit; it is their product that runs past the allowance.
    const nestedQuery = gql`
        query {
            jcr {
                nodeByPath(path: "${TEST_ROOT}") {
                    descendants {
                        nodes {
                            name
                            descendants {
                                nodes { name }
                            }
                        }
                    }
                }
            }
        }
    `;

    const setRequestNodeLimit = (limit: number) => {
        cy.executeGroovy('groovy/setRequestNodeLimit.groovy', {REQUEST_NODE_LIMIT: String(limit)});
    };

    const hasAllowanceError = (errors: any[]) =>
        Boolean(errors?.some((e: any) => e.message.includes('which is the maximum allowed')));

    // Poll until the query is refused for exceeding the allowance (the new value has propagated).
    const waitUntilRejected = (query: any) => {
        cy.waitUntil(
            () => cy.apollo({query, errorPolicy: 'all'}).then((r: any) => hasAllowanceError(r?.errors)),
            {...waitOptions, errorMsg: 'Query was never refused for exceeding the node allowance'}
        );
    };

    // Poll until the query runs without an allowance error AND returns nodes, so that a query which is merely accepted
    // but answers with nothing does not count as a pass.
    const waitUntilAccepted = (query: any) => {
        cy.waitUntil(
            () => cy.apollo({query, errorPolicy: 'all'}).then((r: any) =>
                Boolean(r?.data?.jcr?.nodeByPath?.descendants?.nodes?.length) && !hasAllowanceError(r?.errors)),
            {...waitOptions, errorMsg: 'Query was never accepted after relaxing the allowance'}
        );
    };

    before('create the fixture subtree', () => {
        cy.executeGroovy('groovy/prepareRequestNodeLimitTest.groovy', {});
    });

    after('restore the shipped default and clean up', () => {
        setRequestNodeLimit(SHIPPED_REQUEST_NODE_LIMIT);
        waitUntilAccepted(nestedQuery);
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["${TEST_ROOT}"]) {
                            delete
                        }
                    }
                }
            `
        });
    });

    it('refuses a nested query whose connections are each within the per-connection limit', () => {
        setRequestNodeLimit(200);
        waitUntilRejected(nestedQuery);
    });

    it('still serves the same subtree when it is not nested', () => {
        setRequestNodeLimit(200);
        // Same content, same per-connection limit, same depth budget - only the nesting differs, and that is what the
        // allowance charges for.
        waitUntilAccepted(oneLevelQuery);
    });

    it('reports the limit that was hit', () => {
        setRequestNodeLimit(200);
        waitUntilRejected(nestedQuery);
        cy.apollo({query: nestedQuery, errorPolicy: 'all'}).should((response: any) => {
            expect(response.errors[0].message).to.match(/asked for more than 200 nodes/);
        });
    });

    it('charges totalCount for every node the connection matches, not just the page', () => {
        // 155 matches against an allowance of 100: the one-node page is fine, the count is not.
        setRequestNodeLimit(100);
        waitUntilRejected(totalCountQuery);
    });

    it('serves the same page once totalCount is omitted', () => {
        setRequestNodeLimit(100);
        // Rejection of the totalCount variant doubles as proof that the new limit has propagated.
        waitUntilRejected(totalCountQuery);
        cy.apollo({query: firstPageOnlyQuery, errorPolicy: 'all'}).should((response: any) => {
            expect(hasAllowanceError(response?.errors)).to.equal(false);
            expect(response?.data?.jcr?.nodeByPath?.descendants?.nodes).to.have.length(1);
        });
    });

    it('applies no bound at all when set to 0', () => {
        setRequestNodeLimit(0);
        waitUntilAccepted(nestedQuery);
    });
});
