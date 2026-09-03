import gql from 'graphql-tag';

/*
 * Query-cost guards (graphql.query.maxComplexity / graphql.query.maxDepth / graphql.query.maxExpandedFields).
 *
 * The guards reject a document *before* execution when its estimated cost exceeds the configured limit. They are only
 * honoured when configured on the default provider configuration, so each test drives the limits through a groovy
 * provisioning fixture that edits the "default" factory instance (see setQueryCostLimits.groovy and
 * setExpandedFieldLimit.groovy), then sends a query that exceeds the configured value and asserts the rejection.
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
    const SHIPPED_MAX_EXPANDED_FIELDS = 2000;

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

    const setExpandedFieldLimit = (maxExpandedFields: number) => {
        cy.executeGroovy('groovy/setExpandedFieldLimit.groovy', {MAX_EXPANDED_FIELDS: String(maxExpandedFields)});
    };

    // `levels` fragments, each spreading the one below it twice under distinct aliases, over `parent`. The document
    // grows by one fragment per level while what it executes doubles: 2 * levels + 3 fields as written, 3 * 2^levels
    // once every fragment is expanded at each place it is spread.
    const twiceSpreadFragments = (levels: number) => {
        let document = 'fragment f0 on JCRNode { name } ';
        for (let i = 1; i <= levels; i++) {
            document += `fragment f${i} on JCRNode { x: parent { ...f${i - 1} } y: parent { ...f${i - 1} } } `;
        }

        return gql(document + `{ jcr { nodeByPath(path: "/") { ...f${levels} } } }`);
    };

    const hasCostError = (errors: any[], messageFragment: string) =>
        Boolean(errors?.some((e: any) => e.message.includes(messageFragment)));

    const hasAnyCostError = (errors: any[]) =>
        hasCostError(errors, 'maximum query') || hasCostError(errors, 'Maximum field count');

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
                Boolean(r?.data && dataPath(r.data)) && !hasAnyCostError(r?.errors)),
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
        setExpandedFieldLimit(SHIPPED_MAX_EXPANDED_FIELDS);
        waitUntilAccepted(overComplexQuery, data => data.currentUser);
        waitUntilRejected(twiceSpreadFragments(10), 'Maximum field count exceeded');
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

    /*
     * Fragments. A traversal of the document reads a fragment definition once however many spreads point at it, which
     * is what keeps the complexity and depth guards linear in the size of the document. Execution expands a fragment at
     * every spread, so the fields that run are a separate count, bounded by graphql.query.maxExpandedFields: the
     * operation is built as execution would run it, with the build stopped at the limit, so the measure costs at most
     * the limit and refuses the document before any field is fetched.
     */
    describe('fields executed once fragments are expanded', () => {
        // Eight levels: 19 fields as written, depth 11, 768 once expanded.
        const eightLevels = twiceSpreadFragments(8);
        // Ten levels: 23 fields as written, depth 13, 3072 once expanded - over the shipped limit.
        const tenLevels = twiceSpreadFragments(10);
        // 25 aliases of one scalar: 26 fields as written and 26 once expanded, more than eightLevels writes.
        const flatQuery = gql(`{ currentUser { ${Array.from({length: 25}, (_, i) => `a${i}: displayName`).join(' ')} } }`);

        it('bounds what an operation expands to, not what it writes', () => {
            setLimits(SHIPPED_MAX_COMPLEXITY, SHIPPED_MAX_DEPTH);
            setExpandedFieldLimit(100);
            waitUntilRejected(eightLevels, 'Maximum field count exceeded');
            // A document writing more fields than eightLevels, but expanding to no more than it writes, is served.
            cy.apollo({query: flatQuery}).should((response: any) => {
                expect(response.data.currentUser).to.not.be.null;
            });
        });

        it('rejects an operation over the shipped limit with a single error', () => {
            setLimits(SHIPPED_MAX_COMPLEXITY, SHIPPED_MAX_DEPTH);
            setExpandedFieldLimit(SHIPPED_MAX_EXPANDED_FIELDS);
            waitUntilRejected(tenLevels, 'Maximum field count exceeded');
            cy.apollo({query: tenLevels, errorPolicy: 'all'}).should((response: any) => {
                expect(response.errors).to.have.length(1);
                // The count stops one past the limit, so that is the value the message reports.
                expect(response.errors[0].message).to.equal(
                    `Maximum field count exceeded. ${SHIPPED_MAX_EXPANDED_FIELDS + 1} > ${SHIPPED_MAX_EXPANDED_FIELDS}`
                );
                expect(response.data).to.be.null;
            });
        });

        it('lifts the bound when the property is set to 0', () => {
            setExpandedFieldLimit(100);
            waitUntilRejected(eightLevels, 'Maximum field count exceeded');
            setExpandedFieldLimit(0);
            waitUntilAccepted(eightLevels, data => data.jcr);
        });
    });
});
