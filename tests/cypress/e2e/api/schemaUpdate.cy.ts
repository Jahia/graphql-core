/**
 * SchemaUpdate.cy.ts
 *
 * Verifies that DXGraphQLProvider correctly rebuilds the GraphQL schema when
 * DXGraphQLExtensionsProvider OSGi services appear / disappear, and that
 * DXGraphQLConfig runtime permissions are enforced without a schema rebuild.
 *
 * Module: schema-update-test-module (must be pre-installed ACTIVE).
 *
 * Only one provider can be active at a time (mutual exclusion via switchToProvider).
 *
 * ─── Field inventory ─────────────────────────────────────────────────────────
 *
 *  Shared fields (same GraphQL name, different annotations between A and B):
 *
 *   schemaUpdatePing(msg)      A: description "Ping operation (version A)", open → "pong-A: <msg>"
 *                              B: description "Ping operation (version B)", open → "pong-B: <msg>"
 *                              Tests: description update on schema switch
 *
 *   schemaUpdateGated(msg)     A: @GraphQLRequiresPermission("schemaUpdateNonExistent") → denied
 *                              B: no permission → "gated-B: <msg>"
 *                              Tests: permission removed on schema switch
 *
 *   schemaUpdateNodeField      A: open → "node-A::<name>"
 *  (on JCRNode)                B: @GraphQLRequiresPermission("schemaUpdateNonExistent") → denied
 *                              Tests: permission added on schema switch
 *
 *  Provider-specific fields:
 *
 *   schemaUpdateAOnly(msg)     A only → "a-only: <msg>"     Tests: field disappears
 *   schemaUpdateBOnly(msg)     B only → "b-only: <msg>"     Tests: field appears
 *
 * ─── Test scenarios ───────────────────────────────────────────────────────────
 *   1. No providers active     – all fields absent from schema
 *   2. Provider A active       – verify A-specific values, descriptions, denied gated, absent B-only
 *   3. Switch to Provider B    – verify B-specific values, description change, gated now open,
 *                                nodeField now denied, absent A-only, present B-only
 *   4. DXGraphQLConfig perm    – add runtime permission on schemaUpdatePing → denied → remove → open
 *   5. Deactivate Provider B   – all fields absent
 *   6. Real Jahia permission   – schemaUpdatePermTest requires 'schemaUpdateTestAccess'; user with
 *                                the role can access it, user without the role is denied
 */

import gql from 'graphql-tag';
import {addNode, deleteNode, createUser, deleteUser, grantRoles, publishAndWaitJobEnding} from '@jahia/cypress';
import {
    activateProvider,
    deactivateProvider,
    addGraphQLPermissionConfig,
    cleanupGraphQLPermissionConfig,
    addAuthorizationConfig,
    cleanupAuthorizationConfig,
    waitForFieldInSchema
} from '../../fixtures/moduleManagement';

// ─── Constants ────────────────────────────────────────────────────────────────

const PROVIDER_A_PID = 'org.jahia.test.graphql.schemaupdate.providerA';
const PROVIDER_B_PID = 'org.jahia.test.graphql.schemaupdate.providerB';
const ALL_PROVIDER_PIDS = [PROVIDER_A_PID, PROVIDER_B_PID];

const TEST_PERMISSION = 'schemaUpdateTestAccess';
const TEST_ROLE = 'schemaUpdateTestRole';
const PERM_USER = {username: 'schemaUpdatePermUser', password: 'testPass1234'};
const NO_PERM_USER = {username: 'schemaUpdateNoPermUser', password: 'testPass1234'};

const ROOT_USER = {username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') || 'root1234'};
const TEST_NODE_PATH = '/schemaUpdateTestNode';

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Activate a provider while deactivating all others (mutual exclusion). */
const switchToProvider = (pid: string): void => {
    activateProvider(pid, ALL_PROVIDER_PIDS.filter(p => p !== pid));
};

/** Assert the field is absent from schema (GraphQL validation error or no data). */
const expectFieldAbsent = (queryStr: string): void => {
    cy.apolloClient(ROOT_USER).apollo({
        query: gql(queryStr),
        errorPolicy: 'all'
    }).should((resp: any) => {
        const errors: any[] = resp.errors ?? [];
        expect(
            errors.length > 0 || resp.data === null,
            `Expected field to be absent (schema validation error) for:\n  ${queryStr}`
        ).to.be.true;
    });
};

/** Assert the response contains a Jahia access-denied error. */
const expectAccessDenied = (queryStr: string, user = ROOT_USER): void => {
    cy.apolloClient(user).apollo({
        query: gql(queryStr),
        errorPolicy: 'all'
    }).should((resp: any) => {
        const errors: any[] = resp.errors ?? [];
        expect(errors.length, `Expected access-denied error for:\n  ${queryStr}`).to.be.greaterThan(0);
        const denied = errors.some((e: any) =>
            e.message === 'Permission denied' || e.errorType === 'GqlAccessDeniedException'
        );
        expect(denied, `Expected GqlAccessDeniedException. Got: ${JSON.stringify(errors)}`).to.be.true;
    });
};

/** Read the description of a named field from the Query type via introspection. */
const getFieldDescription = (fieldName: string): Cypress.Chainable<string> =>
    cy.apolloClient(ROOT_USER).apollo({
        queryFile: 'schemaUpdate/introspectQueryFieldDescriptions.graphql'
    }).then((resp: any) => {
        const field = (resp.data.__type.fields as {name: string; description: string}[])
            .find((f: any) => f.name === fieldName);
        return field?.description ?? '';
    });

/** Extract field names from a __type introspection response. */
const extractFieldNames = (resp: any): string[] =>
    (resp.data.__type.fields as {name: string}[]).map((f: any) => f.name);

// ─── Test Suite ───────────────────────────────────────────────────────────────

describe('Schema Update – DXGraphQLExtensionsProvider lifecycle and DXGraphQLConfig', () => {
    before('Ensure both providers deactivated; create JCR test node; cleanup leftover configs', () => {
        ALL_PROVIDER_PIDS.forEach(pid => deactivateProvider(pid));
        cleanupGraphQLPermissionConfig();
        // Deploy authorization YAML — grants API-level access to schemaUpdatePermTest
        // for all hosted-origin users. @GraphQLRequiresPermission then gates by role.
        addAuthorizationConfig();

        // Idempotent cleanup of any leftover state from a previous interrupted run
        cy.apolloClient(ROOT_USER).apollo({
            mutation: gql`mutation { jcr { deleteNode(pathOrId: "/roles/${TEST_ROLE}") } }`,
            errorPolicy: 'all'
        });
        cy.apolloClient(ROOT_USER).apollo({
            mutation: gql`mutation { jcr { deleteNode(pathOrId: "/permissions/${TEST_PERMISSION}") } }`,
            errorPolicy: 'all'
        });
        deleteUser(PERM_USER.username);
        deleteUser(NO_PERM_USER.username);

        // Create the real permission node in JCR
        addNode({parentPathOrId: '/permissions', name: TEST_PERMISSION, primaryNodeType: 'jnt:permission'});

        // Create a role and assign the permission to it
        addNode({parentPathOrId: '/roles', name: TEST_ROLE, primaryNodeType: 'jnt:role'});
        cy.apolloClient(ROOT_USER).apollo({
            mutation: gql`
                    mutation {
                        jcr {
                            mutateNode(pathOrId: "/roles/${TEST_ROLE}") {
                                mutateProperty(name: "j:permissionNames") {
                                    setValues(values: ["${TEST_PERMISSION}"])
                                }
                            }
                        }
                    }
                `
        });

        // Create users
        createUser(PERM_USER.username, PERM_USER.password);
        createUser(NO_PERM_USER.username, NO_PERM_USER.password);

        // Grant the role to PERM_USER at the repository root (default workspace)
        grantRoles('/', [TEST_ROLE], PERM_USER.username, 'USER');

        addNode({parentPathOrId: '/', name: 'schemaUpdateTestNode', primaryNodeType: 'jnt:contentList'});
        publishAndWaitJobEnding('/schemaUpdateTestNode');
    });

    after('Remove JCR test node; deactivate all providers; cleanup permission configs', () => {
        ALL_PROVIDER_PIDS.forEach(pid => deactivateProvider(pid));
        cleanupGraphQLPermissionConfig();
        cleanupAuthorizationConfig();
        deleteUser(PERM_USER.username);
        deleteUser(NO_PERM_USER.username);
        deleteNode(`/roles/${TEST_ROLE}`);
        deleteNode(`/permissions/${TEST_PERMISSION}`);
        deleteNode(TEST_NODE_PATH);
    });

    // ── Scenario 1: No providers active ──────────────────────────────────────

    describe('Scenario 1 – no providers active: all module fields absent from schema', () => {
        it('schemaUpdatePing is absent', () => {
            expectFieldAbsent('query { schemaUpdatePing(msg: "test") }');
        });

        it('schemaUpdateGated is absent', () => {
            expectFieldAbsent('query { schemaUpdateGated(msg: "test") }');
        });

        it('schemaUpdateAOnly is absent', () => {
            expectFieldAbsent('query { schemaUpdateAOnly(msg: "test") }');
        });

        it('schemaUpdateBOnly is absent', () => {
            expectFieldAbsent('query { schemaUpdateBOnly(msg: "test") }');
        });

        it('schemaUpdateNodeField is absent', () => {
            expectFieldAbsent(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeField } } }`);
        });

        it('schemaUpdatePermTest is absent', () => {
            expectFieldAbsent('query { schemaUpdatePermTest(msg: "test") }');
        });

        it('core JCR fields are unaffected', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { name } } }`
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.name).to.equal('schemaUpdateTestNode');
            });
        });
    });

    // ── Scenario 2: Provider A active ────────────────────────────────────────

    describe('Scenario 2 – Provider A active', () => {
        before('Switch to Provider A', () => {
            switchToProvider(PROVIDER_A_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', true);
        });

        after('Deactivate Provider A', () => {
            deactivateProvider(PROVIDER_A_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', false);
        });

        it('schemaUpdatePing returns version-A value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePing).to.equal('pong-A: hello');
            });
        });

        it('schemaUpdatePing has version-A description', () => {
            getFieldDescription('schemaUpdatePing').should('equal', 'Ping operation (version A)');
        });

        it('schemaUpdateGated is denied (annotation-level non-existent permission in A)', () => {
            expectAccessDenied('query { schemaUpdateGated(msg: "hello") }');
        });

        it('schemaUpdateNodeField returns version-A value (no permission in A)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateNodeField.graphql',
                variables: {path: TEST_NODE_PATH}
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.schemaUpdateNodeField).to.equal('node-A::schemaUpdateTestNode');
            });
        });

        it('schemaUpdateAOnly returns expected value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateAOnly.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdateAOnly).to.equal('a-only: hello');
            });
        });

        it('schemaUpdateBOnly is absent (Provider B not active)', () => {
            expectFieldAbsent('query { schemaUpdateBOnly(msg: "test") }');
        });
    });

    // ── Scenario 3: Switch to Provider B ─────────────────────────────────────

    describe('Scenario 3 – switch to Provider B: verifies all schema-change cases', () => {
        before('Activate A, then switch to B (deactivates A, activates B)', () => {
            switchToProvider(PROVIDER_A_PID);
            waitForFieldInSchema('Query', 'schemaUpdateAOnly', true);
            switchToProvider(PROVIDER_B_PID);
            // Wait for a B-specific field to confirm schema rebuild completed
            waitForFieldInSchema('Query', 'schemaUpdateBOnly', true);
            // Confirm A-specific field is gone
            waitForFieldInSchema('Query', 'schemaUpdateAOnly', false);
        });

        after('Deactivate Provider B', () => {
            deactivateProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', false);
        });

        // ── Description change ──────────────────────────────────────────────

        it('[description update] schemaUpdatePing returns version-B value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePing).to.equal('pong-B: hello');
            });
        });

        it('[description update] schemaUpdatePing has version-B description', () => {
            getFieldDescription('schemaUpdatePing').should('equal', 'Ping operation (version B)');
        });

        // ── Permission removed ──────────────────────────────────────────────

        it('[permission removed] schemaUpdateGated is now accessible (no permission in B)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateGated.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdateGated).to.equal('gated-B: hello');
            });
        });

        // ── Permission added ────────────────────────────────────────────────

        it('[permission added] schemaUpdateNodeField is now denied (non-existent permission in B)', () => {
            expectAccessDenied(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeField } } }`);
        });

        // ── Field appears ───────────────────────────────────────────────────

        it('[field appears] schemaUpdateBOnly is present and returns expected value', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdateBOnly.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdateBOnly).to.equal('b-only: hello');
            });
        });

        // ── Field disappears ────────────────────────────────────────────────

        it('[field disappears] schemaUpdateAOnly is absent after switching to B', () => {
            expectFieldAbsent('query { schemaUpdateAOnly(msg: "test") }');
        });

        // ── Mutual exclusion sanity ─────────────────────────────────────────

        it('[mutual exclusion] only Provider B fields are in schema (no A-only leftovers)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/introspectQueryFieldDescriptions.graphql'
            }).should((resp: any) => {
                const names = extractFieldNames(resp);
                expect(names).to.include('schemaUpdatePing');
                expect(names).to.include('schemaUpdateGated');
                expect(names).to.include('schemaUpdateBOnly');
                expect(names).not.to.include('schemaUpdateAOnly');
            });
        });
    });

    // ── Scenario 4: DXGraphQLConfig runtime permissions ───────────────────────

    describe('Scenario 4 – DXGraphQLConfig: runtime permission applied and removed without schema rebuild', () => {
        before('Switch to Provider B (schemaUpdatePing is open in B)', () => {
            switchToProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', true);
        });

        after('Cleanup permission configs; deactivate Provider B', () => {
            cleanupGraphQLPermissionConfig();
            deactivateProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', false);
        });

        it('schemaUpdatePing is accessible before any config permission', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePing).to.equal('pong-B: hello');
            });
        });

        it('schemaUpdatePing is denied after adding a non-existent config-based permission', () => {
            addGraphQLPermissionConfig('Query.schemaUpdatePing', 'schemaUpdateNonExistentFromCfg');
            // Field remains in schema — only runtime enforcement changes
            expectAccessDenied('query { schemaUpdatePing(msg: "hello") }');
        });

        it('schemaUpdatePing field is still present in schema while denied (no rebuild occurred)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/introspectQueryFieldDescriptions.graphql'
            }).should((resp: any) => {
                const names = extractFieldNames(resp);
                expect(names).to.include('schemaUpdatePing');
            });
        });

        it('schemaUpdatePing is accessible again after removing the config permission', () => {
            cleanupGraphQLPermissionConfig();
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePing.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePing).to.equal('pong-B: hello');
            });
        });
    });

    // ── Scenario 5: Deactivate Provider B ────────────────────────────────────

    describe('Scenario 5 – Provider B deactivated: all module fields absent from schema', () => {
        before('Activate then deactivate Provider B', () => {
            switchToProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', true);
            deactivateProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePing', false);
        });

        it('schemaUpdatePing is absent', () => {
            expectFieldAbsent('query { schemaUpdatePing(msg: "test") }');
        });

        it('schemaUpdateGated is absent', () => {
            expectFieldAbsent('query { schemaUpdateGated(msg: "test") }');
        });

        it('schemaUpdateBOnly is absent', () => {
            expectFieldAbsent('query { schemaUpdateBOnly(msg: "test") }');
        });

        it('schemaUpdatePermTest is absent', () => {
            expectFieldAbsent('query { schemaUpdatePermTest(msg: "test") }');
        });

        it('schemaUpdateNodeField is absent', () => {
            expectFieldAbsent(`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { schemaUpdateNodeField } } }`);
        });

        it('core JCR fields remain unaffected', () => {
            cy.apolloClient(ROOT_USER).apollo({
                query: gql`query { jcr { nodeByPath(path: "${TEST_NODE_PATH}") { name } } }`
            }).should((resp: any) => {
                expect(resp.data.jcr.nodeByPath.name).to.equal('schemaUpdateTestNode');
            });
        });
    });

    // ── Scenario 6: Real Jahia permission — user role access control ──────────
    //
    // schemaUpdatePermTest requires the real Jahia permission "schemaUpdateTestAccess".
    // The test creates the permission in JCR, creates a role that grants it, assigns the
    // role to a test user, and verifies:
    //   - root always has access
    //   - user with the role can access the field
    //   - user without the role gets GqlAccessDeniedException

    describe('Scenario 6 – Real Jahia permission: role-based access control on schemaUpdatePermTest', () => {
        before('Activate Provider B; deploy authorization config; create permission, role and users', () => {
            switchToProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePermTest', true);
        });

        after('Deactivate Provider B; remove authorization config, permission, role and users', () => {
            deactivateProvider(PROVIDER_B_PID);
            waitForFieldInSchema('Query', 'schemaUpdatePermTest', false);
        });

        it('root can access schemaUpdatePermTest (root bypasses all permission checks)', () => {
            cy.apolloClient(ROOT_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePermTest.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePermTest).to.equal('perm-test: hello');
            });
        });

        it('user with role can access schemaUpdatePermTest', () => {
            cy.apolloClient(PERM_USER).apollo({
                queryFile: 'schemaUpdate/schemaUpdatePermTest.graphql',
                variables: {msg: 'hello'}
            }).should((resp: any) => {
                expect(resp.data.schemaUpdatePermTest).to.equal('perm-test: hello');
            });
        });

        it('user without role is denied access to schemaUpdatePermTest', () => {
            expectAccessDenied('query { schemaUpdatePermTest(msg: "hello") }', NO_PERM_USER);
        });
    });
});
