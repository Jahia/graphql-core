/**
 * ModuleManagement.ts
 *
 * Utilities to manage Jahia OSGi component lifecycle and schema changes during Cypress tests.
 *
 * Provider lifecycle (ConfigAdmin-based):
 *   activateProvider(pid)   – creates an empty OSGi config satisfying configurationPolicy=REQUIRE
 *   deactivateProvider(pid) – deletes that config, causing the component to deactivate
 *
 * DXGraphQLConfig runtime permissions:
 *   addGraphQLPermissionConfig(key, value) – adds a factory config with a permission entry
 *   cleanupGraphQLPermissionConfig()       – removes all test-marker factory configs
 *
 * Schema-change detection (introspection-based):
 *   waitForFieldInSchema(typeName, fieldName, present) – polls until field appears/disappears
 *
 */

import gql from 'graphql-tag';

/**
 * Activate a DXGraphQLExtensionsProvider component that requires a configuration
 * (configurationPolicy = REQUIRE). Creates an empty ConfigAdmin config for the given PID.
 * Idempotent: does nothing if the config already exists.
 *
 * @param providerPid      PID of the provider to activate.
 * @param deactivateBefore PIDs of other providers to deactivate first (mutual exclusion).
 */
export const activateProvider = (providerPid: string, deactivateBefore: string[] = []): void => {
    cy.executeGroovy('groovy/activateProvider.groovy', {
        PROVIDER_PID: providerPid,
        PROVIDERS_TO_DEACTIVATE: deactivateBefore.join('|')
    });
};

/**
 * Deactivate a DXGraphQLExtensionsProvider component by deleting its ConfigAdmin config.
 * Idempotent: does nothing if the config does not exist.
 */
export const deactivateProvider = (providerPid: string): void => {
    cy.executeGroovy('groovy/deactivateProvider.groovy', {PROVIDER_PID: providerPid});
};

// ─── DXGraphQLConfig runtime permissions ─────────────────────────────────────
// NOTE: permissions are managed via Groovy scripts that call
// ConfigurationAdmin.createFactoryConfiguration() directly.
// The GraphQL admin mutation (ConfigService.getConfig) creates SINGLETON configs,
// which do NOT trigger ManagedServiceFactory.updated() — only createFactoryConfiguration()
// produces a proper factory config instance that ConfigAdmin routes to the factory.

/**
 * Add a runtime permission entry to DXGraphQLConfig via a new factory config instance.
 * Does NOT rebuild the schema — permission is enforced on every query execution.
 * The config is tagged with "test.schemaupdate=true" so cleanupGraphQLPermissionConfig
 * can find and delete it.
 *
 * @param permissionKey   Key without "permission." prefix, e.g. "Query.schemaUpdatePing"
 * @param permissionValue Permission name to require, e.g. "schemaUpdateNonExistent"
 */
export const addGraphQLPermissionConfig = (permissionKey: string, permissionValue: string): void => {
    cy.executeGroovy('groovy/addGraphQLPermissionConfig.groovy', {
        PERMISSION_KEY: `permission.${permissionKey}`,
        PERMISSION_VALUE: permissionValue
    });
};

/**
 * Remove all DXGraphQLConfig factory config instances created by addGraphQLPermissionConfig.
 * Identified by the "test.schemaupdate=true" marker. Safe to call even when none exist.
 */
export const cleanupGraphQLPermissionConfig = (): void => {
    cy.executeGroovy('groovy/cleanupGraphQLPermissionConfig.groovy');
};

// ─── API authorization config ─────────────────────────────────────────────────
// The org.jahia.bundles.api.authorization YAML files define API-level access
// scopes that gate which GraphQL fields a user may call, independently of
// JCR @GraphQLRequiresPermission checks.

/**
 * Write the authorization YAML file that grants API access to the
 * schemaUpdatePermTest GraphQL field for hosted-origin requests.
 * This is the API-layer gate; @GraphQLRequiresPermission provides the second layer.
 * Idempotent — overwrites any existing file.
 */
export const addAuthorizationConfig = (): void => {
    cy.executeGroovy('groovy/addAuthorizationConfig.groovy');
};

/**
 * Delete the authorization YAML file written by addAuthorizationConfig.
 * Idempotent — safe to call even when the file does not exist.
 */
export const cleanupAuthorizationConfig = (): void => {
    cy.executeGroovy('groovy/cleanupAuthorizationConfig.groovy');
};

// ─── Schema-change detection ──────────────────────────────────────────────────

const ROOT_FOR_WAIT = {username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') || 'root1234'};

/**
 * Wait until a named field appears or disappears in a GraphQL type.
 * Uses __type introspection — works regardless of field-level permissions.
 *
 * @param typeName   GraphQL type name, e.g. "Query" or "JCRNode"
 * @param fieldName  Field name to check, e.g. "schemaUpdateAPing"
 * @param present    true = wait until present; false = wait until absent
 * @param timeoutMs  Max wait time in ms (default 30 000)
 */
export const waitForFieldInSchema = (
    typeName: string,
    fieldName: string,
    present: boolean,
    timeoutMs = 30000
): Cypress.Chainable =>
    cy.waitUntil(
        () =>
            cy.apolloClient(ROOT_FOR_WAIT).apollo({
                query: gql`query { __type(name: "${typeName}") { fields { name } } }`,
                errorPolicy: 'all'
            }).then((resp: any) => {
                const fields: {name: string}[] = resp.data?.__type?.fields ?? [];
                return fields.some((f: any) => f.name === fieldName) === present;
            }),
        {
            timeout: timeoutMs,
            interval: 2000,
            errorMsg: `Field '${fieldName}' on type '${typeName}' did not ${present ? 'appear' : 'disappear'} within ${timeoutMs}ms`
        }
    );
