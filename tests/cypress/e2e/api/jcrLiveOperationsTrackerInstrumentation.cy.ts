import {createSite, deleteSite} from '@jahia/cypress';

describe('Test tracker instrumentation (setting HTTP response headers)', () => {
    const WORKSPACE_EDIT = 'EDIT';
    const WORKSPACE_LIVE = 'LIVE';
    const OPERATION_QUERY = 'query';
    const OPERATION_MUTATION = 'mutation';
    const SITE_NAME = 'jcrLiveOperationsTrackerInstrumentation';
    const testDataMultiJCR = [
        {w1: WORKSPACE_EDIT, w2: WORKSPACE_EDIT, expected: false},
        {w1: WORKSPACE_LIVE, w2: WORKSPACE_LIVE, expected: true},
        {w1: WORKSPACE_EDIT, w2: WORKSPACE_LIVE, expected: true},
        {w1: WORKSPACE_LIVE, w2: WORKSPACE_EDIT, expected: true}
    ];

    beforeEach('Create a test site and stub an interceptor', () => {
        createSite(SITE_NAME);
        // Use an interceptor to access the HTTP response headers:
        cy.intercept('POST', '/modules/graphql').as('graphql');
    });

    afterEach(() => {
        deleteSite(SITE_NAME);
    });

    // --------
    // Queries:
    // --------

    it('GIVEN a query with the default JCR workspace WHEN executed THEN the http header is not set', () => {
        executeJcrQuery('querySingleJcr.graphql');

        shouldNotHaveHeader();
    });

    it('GIVEN a query with the JCR edit workspace WHEN executed THEN the http header is not set', () => {
        executeJcrQuery('querySingleJcr.graphql', WORKSPACE_EDIT);

        shouldNotHaveHeader();
    });

    it('GIVEN a query with the JCR live workspace WHEN executed THEN the http header is set and matches', () => {
        executeJcrQuery('querySingleJcr.graphql', WORKSPACE_LIVE);

        shouldHaveHeaderValue(OPERATION_QUERY);
    });

    it('GIVEN a query not using JCR WHEN executed THEN the http header is not set', () => {
        executeNonJcrQuery();

        shouldNotHaveHeader();
    });

    testDataMultiJCR.forEach(test => {
        it(`GIVEN a query with multiple JCR (${test.w1}, ${test.w2}) WHEN executed THEN the http header matches the expectation (${test.expected})`, () => {
            executeMultiJcrQuery(test);
            if (test.expected) {
                shouldHaveHeaderValue(OPERATION_QUERY);
            } else {
                shouldNotHaveHeader();
            }
        });
    });

    // ----------
    // Mutations:
    // ----------

    it('GIVEN a mutation with the default JCR workspace WHEN executed THEN the http header is not set', () => {
        executeJcrMutation('mutationSingleJcr.graphql');

        shouldNotHaveHeader();
    });

    it('GIVEN a mutation with the JCR edit workspace WHEN executed THEN the http header is not set', () => {
        executeJcrMutation('mutationSingleJcr.graphql', WORKSPACE_EDIT);

        shouldNotHaveHeader();
    });

    it('GIVEN a mutation with the JCR live workspace WHEN executed THEN the http header is set and matches', () => {
        executeJcrMutation('mutationSingleJcr.graphql', WORKSPACE_LIVE);

        shouldHaveHeaderValue(OPERATION_MUTATION);
    });

    it('GIVEN a mutation not using JCR WHEN executed THEN the http header is not set', () => {
        executeNonJcrMutation();

        shouldNotHaveHeader();
    });

    testDataMultiJCR.forEach(test => {
        it(`GIVEN a mutation with multiple JCR (${test.w1}, ${test.w2}) WHEN executed THEN the http header matches the expectation (${test.expected})`, () => {
            executeMultiJcrMutation(test);
            if (test.expected) {
                shouldHaveHeaderValue(OPERATION_MUTATION);
            } else {
                shouldNotHaveHeader();
            }
        });
    });

    // --------
    // helpers:
    // --------

    function executeJcrQuery(filename: string, workspace?: string) {
        const variables = workspace ? {workspace: workspace} : undefined;
        cy.apollo({queryFile: `jcrLiveOperationsTrackerInstrumentation/${filename}`, variables: variables}).then(
            result => {
                // Ensure the operation completes
                const jcrOperation = result?.data?.jcr;
                expect(jcrOperation).to.have.property('workspace', workspace ? workspace : 'EDIT');
            }
        );
    }

    function executeNonJcrQuery() {
        cy.apollo({queryFile: 'jcrLiveOperationsTrackerInstrumentation/queryNonJcr.graphql'}).then(result => {
            // Ensure the operation completes
            expect(result?.data?.currentUser?.displayName).to.exist;
        });
    }

    function executeMultiJcrQuery(test: {w1: string; w2: string; expected: boolean}) {
        cy.apollo({
            queryFile: 'jcrLiveOperationsTrackerInstrumentation/queryMultiJcr.graphql',
            variables: {workspace1: test.w1, workspace2: test.w2}
        });
    }

    function executeJcrMutation(filename: string, workspace?: string) {
        // Const variables = getBasicJcrMutationVariables();
        // variables.workspace = workspace;
        const variables = {...getBasicJcrMutationVariables(), name: 'foo', workspace: workspace};
        cy.apollo({
            queryFile: `jcrLiveOperationsTrackerInstrumentation/${filename}`,
            variables: variables
        }).then(result => {
            // Ensure the operation completes
            const addNodeOperation = result?.data?.jcr?.addNode;
            expect(addNodeOperation).to.have.property('createVersion', true);
        });
    }

    function executeNonJcrMutation() {
        cy.apollo({queryFile: 'jcrLiveOperationsTrackerInstrumentation/mutationNonJcr.graphql'}).then(result => {
            // Ensure the operation completes
            expect(result?.data?.jwtToken).to.exist;
        });
    }

    function executeMultiJcrMutation(test: {w1: string; w2: string; expected: boolean}) {
        const variables = {
            ...getBasicJcrMutationVariables(),
            name1: 'foo1',
            name2: 'foo2',
            workspace1: test.w1,
            workspace2: test.w2
        };

        cy.apollo({
            queryFile: 'jcrLiveOperationsTrackerInstrumentation/mutationMultiJcr.graphql',
            variables: variables
        });
    }

    function getBasicJcrMutationVariables() {
        console.log('getting variables');
        return {path: `/sites/${SITE_NAME}`, nodeType: 'jnt:contentList'};
    }

    function shouldHaveHeaderValue(operationValue: string) {
        cy.get('@graphql').its('response.headers').should('have.property', 'x-jahia-live-operation', operationValue);
    }

    function shouldNotHaveHeader() {
        cy.get('@graphql') // Yields the same interception object
            .its('response.headers')
            .should('not.have.property', 'x-jahia-live-operation');
    }
});
