/**
 * SchemaUpdate.cy.ts
 *
 * Verifies that the GraphQL schema is correctly rebuilt when OSGi bundles
 * contributing schema extensions are started and stopped.
 *
 * Both schema-update-test-module v1 (1.0.0-SNAPSHOT) and v2 (2.0.0-SNAPSHOT)
 * must be pre-installed (RESOLVED) on the server before the suite runs.
 * The tests control the bundle lifecycle (start / stop) via Groovy scripts.
 *
 * Field inventory
 * ───────────────
 *   schemaUpdatePing         – open Query field (v2: adds schemaUpdateAdmin permission)
 *   schemaUpdateAdminPing    – Query field requiring 'schemaUpdateAdmin' (v2: arg renamed)
 *   schemaUpdateGhostPing    – Query field requiring 'schemaUpdateNonExistent' (v2: perm removed → open)
 *   schemaUpdateNodeTag      – open JcrNode field (v2: prefix changes)
 *   schemaUpdateNodeSecret   – JcrNode field requiring 'schemaUpdateAdmin' (v2: perm→nonexistent → denied)
 *   schemaUpdateNodeGhost    – JcrNode field requiring 'schemaUpdateNonExistent' (v2: perm removed → open)
 *
 * Scenarios
 * ─────────
 *   1. v1 active   – all fields present; ghost fields denied even for root
 *   2. v1 stopped  – all fields absent from schema
 *   3. v2 active   – permission removals unblock ghost fields; arg rename; impl changes; node secret denied
 *                    (no v2→v1 rollback needed: the v1→v2 changes themselves prove schema was rebuilt)
 */

import gql from 'graphql-tag';
import {addNode, deleteNode} from '@jahia/cypress';
import {startModule, stopModule, stopModuleSafe, waitForModuleState} from '../../fixtures/moduleManagement';

// ─── Constants ────────────────────────────────────────────────────────────────

const MODULE_KEY = 'schema-update-test-module';
const MODULE_V1_VERSION = '1.0.0-SNAPSHOT';
const MODULE_V2_VERSION = '2.0.0-SNAPSHOT';

const ROOT_USER = {username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') || 'root1234'};
const TEST_NODE_PATH = '/schemaUpdateTestNode';

// ─── Lifecycle helpers ────────────────────────────────────────────────────────

const startV1 = () => {
    startModule(MODULE_KEY, MODULE_V1_VERSION);
    waitForModuleState(MODULE_KEY, MODULE_V1_VERSION, 'ACTIVE', 30000);
};

const stopV1 = () => {
    stopModule(MODULE_KEY, MODULE_V1_VERSION);
    waitForModuleState(MODULE_KEY, MODULE_V1_VERSION, 'RESOLVED', 30000);
};

const startV2 = () => {
    startModule(MODULE_KEY, MODULE_V2_VERSION);
    waitForModuleState(MODULE_KEY, MODULE_V2_VERSION, 'ACTIVE', 30000);
};

const stopV2 = () => {
    stopModule(MODULE_KEY, MODULE_V2_VERSION);
    waitForModuleState(MODULE_KEY, MODULE_V2_VERSION, 'RESOLVED', 30000);
};

// ─── Query helpers ────────────────────────────────────────────────────────────

/**
 * Assert a query returns a GraphQL schema error (field/argument absent from schema).
 * Validation errors produce null data and at least one error entry.
 */
const expectFieldAbsent = (queryStr: string) => {
    cy.apolloClient(ROOT_USER).apollo({
        query: gql(queryStr),
        errorPolicy: 'all'
    }).should((resp: any) => {
        const errors: any[] = Array.isArray(resp.errors) ? resp.errors : [];
        expect(
            errors.length > 0 || resp.data === null,
            `Expected a schema validation error (field absent) for: ${queryStr}`
        ).to.be.true;
    });
};

/**
 * Assert a query returns an access-denied error.
 * Jahia throws GqlAccessDeniedException with message "Permission denied".
 * The field resolves to null and the error carries errorType "GqlAccessDeniedException".
 */
const expectAccessDenied = (queryStr: string) => {
    cy.apolloClient(ROOT_USER).apollo({
        query: gql(queryStr),
        errorPolicy: 'all'
    }).should((resp: any) => {
        const errors: any[] = Array.isArray(resp.errors) ? resp.errors : [];
        expect(errors.length, `Expected access-denied error for: ${queryStr}`).to.be.greaterThan(0);
        const hasDenied = errors.some((e: any) =>
            e.message === 'Permission denied' || e.errorType === 'GqlAccessDeniedException'
        );
        expect(hasDenied, `Expected "Permission denied" / GqlAccessDeniedException. Got: ${JSON.stringify(errors)}`).to.be.true;
    });
};

// ─── Test Suite ───────────────────────────────────────────────────────────────

describe('Schema Update – schema-update-test-module lifecycle', () => {
    before('Ensure all test modules stopped, create test JCR node', () => {
        stopModuleSafe(MODULE_KEY, MODULE_V1_VERSION);
        stopModuleSafe(MODULE_KEY, MODULE_V2_VERSION);
        addNode({parentPathOrId: '/', name: 'schemaUpdateTestNode', primaryNodeType: 'jnt:contentList'});
    });

    after('Remove JCR test node', () => {
        deleteNode(TEST_NODE_PATH);
    });

    // ── Scenario 1: v1 active ─────────────────────────────────────────────────

    describe('Scenario 1 – v1 active: all fields present with correct v1 behavior', () => {
        before('Start v1', () => {
            startV1();
        });
        after('Stop v1', () => {
            stopV1();
        });

        it('Open query field returns expected v1 value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {message: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePing).to.equal('pong: hello');
            });
        });

        it('Open query field (arg rename candidate) returns expected v1 value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateAdminPing.graphql',
                variables: {message: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdateAdminPing).to.equal('admin-pong: hello');
            });
        });

        it('Query field with non-existent permission is denied even for root', () => {
            expectAccessDenied('query { schemaUpdateGhostPing(message: "hello") }');
        });

        it('Open node extension field returns expected v1 prefix', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateNodeTag.graphql',
                variables: {path: TEST_NODE_PATH}
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.schemaUpdateNodeTag).to.equal('v1-tag::schemaUpdateTestNode');
            });
        });

        it('Open node extension field (will be permission-gated in v2) returns v1 secret value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateNodeSecret.graphql',
                variables: {path: TEST_NODE_PATH}
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.schemaUpdateNodeSecret).to.match(/^v1-secret::/);
            });
        });

        it('Node field with non-existent permission is denied even for root', () => {
            expectAccessDenied(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeGhost } } }`);
        });

        it('Core JCR fields are unaffected by the module', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { jcr { nodeByPath(path: "/schemaUpdateTestNode") { name } } }`
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.name).to.equal('schemaUpdateTestNode');
            });
        });
    });

    // ── Scenario 2: v1 stopped ────────────────────────────────────────────────

    describe('Scenario 2 – v1 stopped: all module fields absent from schema', () => {
        // Module was stopped by Scenario 1's after hook.

        it('schemaUpdatePing is absent after module stop', () => {
            expectFieldAbsent('query { schemaUpdatePing }');
        });

        it('schemaUpdateAdminPing is absent after module stop', () => {
            expectFieldAbsent('query { schemaUpdateAdminPing }');
        });

        it('schemaUpdateGhostPing is absent after module stop', () => {
            expectFieldAbsent('query { schemaUpdateGhostPing }');
        });

        it('schemaUpdateNodeTag is absent after module stop', () => {
            expectFieldAbsent(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeTag } } }`);
        });

        it('schemaUpdateNodeSecret is absent after module stop', () => {
            expectFieldAbsent(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeSecret } } }`);
        });

        it('schemaUpdateNodeGhost is absent after module stop', () => {
            expectFieldAbsent(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeGhost } } }`);
        });
    });

    // ── Scenario 3: v2 active ─────────────────────────────────────────────────

    describe('Scenario 3 – v2 active: all v2 changes reflected in schema', () => {
        before('Start v2', () => {
            startV2();
        });
        after('Stop v2', () => {
            stopV2();
        });

        it('schemaUpdatePing returns v2 prefix (implementation updated)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {message: 'hello'}
            }).should((resp: any) => {
                // A stale v1 resolver would return "pong: hello"; v2 must return "v2-pong: hello".
                expect(resp.data.schemaUpdatePing).to.equal('v2-pong: hello');
            });
        });

        it('schemaUpdateAdminPing accepts renamed argument "text" and returns v2 prefix', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateAdminPingV2.graphql',
                variables: {text: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdateAdminPing).to.equal('v2-admin-pong: hello');
            });
        });

        it('schemaUpdateAdminPing rejects the v1 "message" argument (schema validation error)', () => {
            // In v2 the argument is renamed to "text"; "message" is unknown to the rebuilt schema.
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateAdminPing.graphql',
                variables: {message: 'old-arg'},
                errorPolicy: 'all'
            }).should((resp: any) => {
                expect(
                    resp.errors,
                    'Expected schema validation error for unknown argument "message" in v2'
                ).to.have.length.greaterThan(0);
            });
        });

        it('schemaUpdateGhostPing is now accessible to root (non-existent permission removed in v2)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { schemaUpdateGhostPing(message: "hello") }`
            }).should((resp: any) => {
                // In v1 this field was always denied; v2 removes the non-existent permission entirely.
                expect(resp.data.schemaUpdateGhostPing).to.equal('v2-ghost-pong: hello');
            });
        });

        it('schemaUpdateNodeTag returns v2 prefix (no stale v1 resolver)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateNodeTag.graphql',
                variables: {path: TEST_NODE_PATH}
            }).should((resp: any) => {
                // A stale v1 resolver would return "v1-tag::"; v2 must return "v2-tag::".
                expect(resp.data.jcr.nodeByPath.schemaUpdateNodeTag).to.equal('v2-tag::schemaUpdateTestNode');
            });
        });

        it('schemaUpdateNodeSecret is now denied for root (non-existent permission added in v2)', () => {
            // In v1 root could call this field; v2 changes the permission to non-existent.
            expectAccessDenied(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeSecret } } }`);
        });

        it('schemaUpdateNodeGhost is now accessible to root (non-existent permission removed in v2)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeGhost } } }`
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.schemaUpdateNodeGhost).to.equal('v2-ghost::schemaUpdateTestNode');
            });
        });

        it('Core JCR fields are unaffected by v2', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { jcr { nodeByPath(path: "/schemaUpdateTestNode") { name } } }`
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.name).to.equal('schemaUpdateTestNode');
            });
        });
    });
});

