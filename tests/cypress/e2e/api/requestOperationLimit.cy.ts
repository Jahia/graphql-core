/*
 * Per-request operation limit (graphql.request.operationLimit).
 *
 * The endpoint accepts a JSON array as a request body, each element of which is an independent operation with its own
 * document and variables, all of them executed as part of the one request. This bound counts those elements, and it is
 * the one limit that is not a measure of a single operation: graphql.query.maxComplexity and graphql.query.maxDepth
 * describe one operation each, and each operation opens its own node and mutation batch allowance, so this is the
 * factor all of them are multiplied by.
 *
 * The count is settled before any element is parsed, so these tests use the cheapest operation there is -
 * `{__typename}` - and a pass or a failure turns on the number of them alone.
 *
 * Requests go through cy.request rather than cy.apollo: an array body is not a shape an Apollo client will send, and it
 * is the raw response - its HTTP status, and how many entries it carries - that the assertions are about. The bound is
 * applied to the request rather than to the schema, so this suite is what shows it reaching the endpoint; the wording
 * of the refusal is asserted where it is deterministic, in RequestOperationLimitPreProcessorTest (a refusal travels
 * back as a container error page, whose rendering of the message is not the endpoint's to promise).
 *
 * Only honoured from the default provider configuration, so the limit is driven through a groovy provisioning fixture
 * that edits the "default" factory instance (see setRequestOperationLimit.groovy). Config propagation (ConfigAdmin
 * update -> ManagedServiceFactory.updated) is asynchronous, so we poll until the new value takes effect rather than
 * waiting a fixed delay.
 */
describe('GraphQL per-request operation limit', () => {
    const waitOptions = {interval: 500, timeout: 30000};

    // The shipped default, restored after the suite so later specs are unaffected.
    const SHIPPED_OPERATION_LIMIT = 20;

    const setOperationLimit = (limit: number) => {
        cy.executeGroovy('groovy/setRequestOperationLimit.groovy', {OPERATION_LIMIT: String(limit)});
    };

    // Posts one request carrying `operations` independent operations, as a JSON array body.
    const postOperations = (operations: number) =>
        cy.request({
            method: 'POST',
            url: '/modules/graphql',
            body: Array.from({length: operations}, () => ({query: '{__typename}'})),
            failOnStatusCode: false
        });

    const waitUntilRefused = (operations: number) => {
        cy.waitUntil(
            () => postOperations(operations).then((r: any) => r.status === 400),
            {...waitOptions, errorMsg: `A request of ${operations} operations was never refused`}
        );
    };

    const waitUntilAccepted = (operations: number) => {
        cy.waitUntil(
            () => postOperations(operations).then((r: any) => r.status === 200 && r.body?.length === operations),
            {...waitOptions, errorMsg: `A request of ${operations} operations was never accepted in full`}
        );
    };

    beforeEach('Authenticate, so the requests below reach the endpoint as a logged-in caller', () => {
        cy.login();
    });

    after('Restore the shipped default limit', () => {
        setOperationLimit(SHIPPED_OPERATION_LIMIT);
        cy.login();
        waitUntilAccepted(SHIPPED_OPERATION_LIMIT);
    });

    it('refuses a request submitting more operations than the limit', () => {
        setOperationLimit(3);
        waitUntilRefused(4);
    });

    it('answers every operation of a request at the limit', () => {
        setOperationLimit(3);
        // Rejection of one operation more doubles as proof that the new limit has propagated.
        waitUntilRefused(4);
        postOperations(3).should((response: any) => {
            expect(response.status).to.equal(200);
            expect(response.body).to.have.length(3);
            response.body.forEach((entry: any) => {
                expect(entry.data.__typename).to.equal('Query');
            });
        });
    });

    it('serves a single-operation request whatever the limit is', () => {
        // The ordinary shape a client sends is one operation per request, so the tightest bound still serves it.
        setOperationLimit(1);
        waitUntilRefused(2);
        postOperations(1).should((response: any) => {
            expect(response.status).to.equal(200);
            expect(response.body).to.have.length(1);
        });
    });

    it('applies no bound at all when set to 0', () => {
        setOperationLimit(0);
        waitUntilAccepted(50);
    });

    it('bounds a request at the shipped default', () => {
        setOperationLimit(SHIPPED_OPERATION_LIMIT);
        waitUntilAccepted(SHIPPED_OPERATION_LIMIT);
        postOperations(SHIPPED_OPERATION_LIMIT + 1).should((response: any) => {
            expect(response.status).to.equal(400);
        });
    });
});
