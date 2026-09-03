/*
 * Per-request operation limit (graphql.request.operationLimit).
 *
 * The endpoint accepts a JSON array as a request body, each element of which is an independent operation with its own
 * document and variables, all of them executed as part of the one request. This bound counts those elements, and it is
 * the one limit that is not a measure of a single operation: graphql.query.maxComplexity and graphql.query.maxDepth
 * describe one operation each, and each operation opens its own node and mutation batch allowance, so this is the
 * factor all of them are multiplied by.
 *
 * The count is settled before any operation is parsed or executed, so these tests use the cheapest operation there
 * is - `{__typename}` - and a pass or a failure turns on the number of them alone.
 *
 * Requests go through cy.request rather than cy.apollo: an array body is not a shape an Apollo client will send, and it
 * is the raw response - its HTTP status, and how many entries it carries - that the assertions are about. The bound is
 * applied to the request rather than to the schema, so this suite is what shows it reaching the endpoint.
 *
 * What the assertions deliberately do NOT read is the content of an accepted request's entries. The claim this bound
 * makes is that a request within it is executed and answered once per operation; what each operation then resolves to
 * belongs to the permission layer, and the root fields of this schema are gated - so an entry carries `errors` rather
 * than `data` depending on who is asking, without saying anything about the bound either way.
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

    it('names the counts it refused on', () => {
        setOperationLimit(3);
        waitUntilRefused(4);
        postOperations(4).should((response: any) => {
            // The refusal reaches the caller as the error page for the status, which quotes the cause. Reading it
            // here is what distinguishes this bound's 400 from any other refusal of the same request.
            expect(String(response.body)).to.contain('4 operations');
            expect(String(response.body)).to.contain('maximum of 3');
        });
    });

    it('answers every operation of a request at the limit', () => {
        setOperationLimit(3);
        // Rejection of one operation more doubles as proof that the new limit has propagated.
        waitUntilRefused(4);
        postOperations(3).should((response: any) => {
            expect(response.status).to.equal(200);
            expect(response.body).to.have.length(3);
            // One result per operation is the whole claim: nothing was dropped on the way through.
            response.body.forEach((entry: any) => {
                expect(entry).to.have.any.keys('data', 'errors');
            });
        });
    });

    it('refuses a request over the limit from a caller who is not logged in', () => {
        setOperationLimit(3);
        // The refusal of the logged-in request is what shows the new limit has propagated; the same request is then
        // sent with no credentials at all, since nothing about the bound depends on who is asking.
        waitUntilRefused(4);
        cy.clearCookies();
        postOperations(4).should((response: any) => {
            expect(response.status).to.equal(400);
            expect(String(response.body)).to.contain('maximum of 3');
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
