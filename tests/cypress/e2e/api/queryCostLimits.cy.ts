import gql from 'graphql-tag';

/*
 * Query-cost guards (graphql.query.maxComplexity / graphql.query.maxDepth).
 *
 * The guards reject a document *before* execution when its estimated cost exceeds the configured limit. They are only
 * honoured when configured on the default provider config file, so each test drives the limits through a groovy
 * provisioning fixture that edits the "default" factory instance (see setQueryCostLimits.groovy), then sends a query
 * that exceeds the configured value and asserts the standard graphql-java rejection.
 *
 * Config propagation (ConfigAdmin update -> fileinstall write-back -> ManagedServiceFactory.updated) is asynchronous,
 * so we poll the endpoint until the new limit takes effect rather than waiting a fixed delay. Setting a guard to 0
 * disables it, which lets each test isolate the guard under test from the other one.
 */
describe('GraphQL query-cost guards', () => {
    const waitOptions = {interval: 500, timeout: 30000};

    // CurrentUser aliased 10x -> complexity = 1 (currentUser) + 10 (scalars) = 11, depth = 2
    const overComplexQuery = gql`
        query {
            currentUser {
                a: displayName b: displayName c: displayName d: displayName e: displayName
                f: displayName g: displayName h: displayName i: displayName j: displayName
            }
        }
    `;

    // Deeply nested parent chain -> depth well beyond 2, low complexity
    const overDeepQuery = gql`
        query {
            jcr {
                nodeByPath(path: "/") {
                    parent { parent { parent { parent { name } } } }
                }
            }
        }
    `;

    // Complexity = 2, depth = 2 -> passes under any generous limit
    const cheapQuery = gql`
        query {
            currentUser {
                username
            }
        }
    `;

    const setLimits = (maxComplexity: number, maxDepth: number) => {
        cy.executeGroovy('groovy/setQueryCostLimits.groovy', {
            MAX_COMPLEXITY: String(maxComplexity),
            MAX_DEPTH: String(maxDepth)
        });
    };

    // Poll until the given query is rejected with the expected abort message (guard has propagated).
    const waitUntilRejected = (query: any, messageFragment: string) => {
        cy.waitUntil(() => cy.apollo({query, errorPolicy: 'all'}).then((response: any) =>
            Boolean(response?.errors?.some((e: any) => e.message.includes(messageFragment)))
        ), {...waitOptions, errorMsg: `Query was never rejected with "${messageFragment}"`});
    };

    // Poll until the given query executes without any cost-guard error (guard relaxed/propagated).
    const waitUntilAccepted = (query: any) => {
        cy.waitUntil(() => cy.apollo({query, errorPolicy: 'all'}).then((response: any) =>
            Boolean(response?.data?.currentUser) &&
            !response?.errors?.some((e: any) => e.message.includes('maximum query'))
        ), {...waitOptions, errorMsg: 'Query was never accepted after relaxing the limits'});
    };

    after('Restore the shipped default limits', () => {
        setLimits(2000, 30);
        waitUntilAccepted(overComplexQuery);
    });

    it('rejects a query exceeding graphql.query.maxComplexity', () => {
        setLimits(5, 0); // Depth guard disabled to isolate the complexity guard
        waitUntilRejected(overComplexQuery, 'maximum query complexity exceeded');
    });

    it('rejects a query exceeding graphql.query.maxDepth', () => {
        setLimits(0, 2); // Complexity guard disabled to isolate the depth guard
        waitUntilRejected(overDeepQuery, 'maximum query depth exceeded');
    });

    it('accepts an in-budget query and reports the offending value in the error', () => {
        setLimits(5, 0);
        // The cheap query (complexity 2) passes even while the aliased query (complexity 11) is rejected.
        waitUntilRejected(overComplexQuery, 'maximum query complexity exceeded');
        cy.apollo({query: cheapQuery}).should((response: any) => {
            expect(response.data.currentUser).to.not.be.null;
        });
        // Error message is actionable: it states the measured value against the limit.
        cy.apollo({query: overComplexQuery, errorPolicy: 'all'}).should((response: any) => {
            expect(response.errors[0].message).to.match(/maximum query complexity exceeded \d+ > 5/);
        });
    });

    it('reverts to the default (guard disabled) when the properties are removed', () => {
        // Enable a strict limit, confirm it is active...
        setLimits(5, 0);
        waitUntilRejected(overComplexQuery, 'maximum query complexity exceeded');
        // ...then remove the properties from the default config. The guard must revert to its code default
        // (0 = disabled) rather than sticking at 5, so the previously-rejected query is accepted again.
        cy.executeGroovy('groovy/removeQueryCostLimits.groovy', {});
        waitUntilAccepted(overComplexQuery);
    });
});
