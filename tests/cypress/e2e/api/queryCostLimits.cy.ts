import gql from 'graphql-tag';

/*
 * Query-cost guards (graphql.query.maxComplexity / graphql.query.maxDepth).
 *
 * The guards reject a document *before* execution when its estimated cost exceeds the configured limit. They are only
 * honoured when configured on the default provider configuration, so each test drives the limits through a groovy
 * provisioning fixture that edits the "default" factory instance (see setQueryCostLimits.groovy), then sends a query
 * that exceeds the configured value and asserts the rejection.
 *
 * Config propagation (ConfigAdmin update -> ManagedServiceFactory.updated) is asynchronous, so we poll the endpoint
 * until the new limit takes effect rather than waiting a fixed delay. Setting a guard to 0 disables it, which lets each
 * test isolate the guard under test from the others.
 */
describe('GraphQL query-cost guards', () => {
    const waitOptions = {interval: 500, timeout: 30000};

    // The shipped defaults, restored after the suite so later specs are unaffected.
    const SHIPPED_MAX_COMPLEXITY = 2000;
    const SHIPPED_MAX_DEPTH = 30;

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

    const hasCostError = (errors: any[], messageFragment: string) =>
        Boolean(errors?.some((e: any) => e.message.includes(messageFragment)));

    // Poll until the given query is rejected with the expected abort message (guard has propagated).
    const waitUntilRejected = (query: any, messageFragment: string) => {
        cy.waitUntil(
            () => cy.apollo({query, errorPolicy: 'all'}).then((r: any) => hasCostError(r?.errors, messageFragment)),
            {...waitOptions, errorMsg: `Query was never rejected with "${messageFragment}"`}
        );
    };

    // Poll until the given query executes without any cost-guard error (guard relaxed/propagated). `dataPath` names a
    // field that must be present in the response, so a query that is merely accepted but returns nothing does not
    // count as a pass.
    const waitUntilAccepted = (query: any, dataPath: (data: any) => unknown) => {
        cy.waitUntil(
            () => cy.apollo({query, errorPolicy: 'all'}).then((r: any) =>
                Boolean(r?.data && dataPath(r.data)) && !hasCostError(r?.errors, 'maximum query')),
            {...waitOptions, errorMsg: 'Query was never accepted after relaxing the limits'}
        );
    };

    // Posts a raw document with no credentials. Going through cy.request rather than cy.apollo keeps the request
    // unauthenticated and lets us assert on the raw response body.
    const postAnonymously = (query: string) => {
        cy.clearCookies();
        return cy.request({
            method: 'POST',
            url: '/modules/graphql',
            body: {query},
            failOnStatusCode: false
        });
    };

    const waitUntilRejectedAnonymously = (query: string, errorMsg: string) => {
        cy.waitUntil(
            () => postAnonymously(query).then((r: any) =>
                hasCostError(r.body?.errors, 'maximum query complexity exceeded')),
            {...waitOptions, errorMsg}
        );
    };

    // Builds `{ a0:__typename a1:__typename ... }`, i.e. one meta field aliased N times.
    const aliasedTypenameQuery = (aliasCount: number) =>
        `{${Array.from({length: aliasCount}, (_, i) => `a${i}:__typename`).join(' ')}}`;

    after('Restore the shipped default limits', () => {
        setLimits(SHIPPED_MAX_COMPLEXITY, SHIPPED_MAX_DEPTH);
        waitUntilAccepted(overComplexQuery, data => data.currentUser);
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
        // ...then remove the properties from the default config. The complexity guard must revert to its code default
        // (0 = disabled) rather than sticking at 5, so the previously-rejected query is accepted again.
        cy.executeGroovy('groovy/removeQueryCostLimits.groovy', {});
        waitUntilAccepted(overComplexQuery, data => data.currentUser);
    });

    /*
     * Alias amplification. __typename is a meta field that graphql-java's own complexity calculator scores as 0
     * whatever calculator it is handed, so a document aliasing it N times scores 0 there and passes ANY budget, while
     * still costing one permission check and one serialized error object per alias. The provider therefore counts
     * complexity itself; these two tests are the regression guard for that.
     */
    describe('alias amplification via meta fields', () => {
        it('counts aliased __typename fields towards the complexity budget', () => {
            setLimits(5, 0);
            // 10 aliases -> complexity 10 > 5.
            waitUntilRejectedAnonymously(aliasedTypenameQuery(10),
                'Aliased __typename document was never charged for its aliases');
        });

        it('rejects a large aliased payload under the shipped defaults, without amplifying the response', () => {
            setLimits(SHIPPED_MAX_COMPLEXITY, SHIPPED_MAX_DEPTH);
            const payload = aliasedTypenameQuery(4000);

            waitUntilRejectedAnonymously(payload,
                'The aliased payload was never rejected under the shipped defaults');

            postAnonymously(payload).should((response: any) => {
                // A single abort error, not one "Permission denied" object per alias, so the response stays smaller
                // than the request instead of being an amplifier.
                expect(response.body.errors).to.have.length(1);
                expect(response.body.errors[0].message)
                    .to.equal(`maximum query complexity exceeded 4000 > ${SHIPPED_MAX_COMPLEXITY}`);
                expect(JSON.stringify(response.body).length).to.be.lessThan(payload.length);
            });
        });
    });
});
