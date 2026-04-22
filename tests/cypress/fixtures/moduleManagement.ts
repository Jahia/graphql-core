/**
 * ModuleManagement.ts
 *
 * Utilities to manage Jahia OSGi bundle lifecycle during Cypress tests.
 *
 * cy.executeGroovy() yields either:
 *   ['.installed']  – script completed without throwing
 *   ['.failed']     – script threw an exception
 *
 * The return value of the Groovy script itself is ignored by the runner.
 *
 * Groovy scripts used:
 *   startModule.groovy      – calls bundle.start(), throws if not found
 *   stopModule.groovy       – calls bundle.stop(), throws if not found
 *   checkModuleState.groovy – throws if bundle is not in EXPECTED_STATE;
 *                             used by waitForModuleState to poll until match
 *
 * For in-place upgrades (Scenario G) the Karaf SSH helper is still provided
 * because bundle:update has no Groovy / REST equivalent.
 */

/// <reference types="cypress-wait-until" />

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Assert that a cy.executeGroovy() result is '.installed' (success).
 * Throws immediately if the script returned '.failed'.
 */
const assertGroovyOk = (result: any, context: string): void => {
    const status: string = Array.isArray(result) ? result[0] : String(result);
    if (status !== '.installed') {
        throw new Error(`[${context}] Groovy script failed (yielded: '${status}'). Check Jahia logs for details.`);
    }
};

// ─── OSGi state constant names used as EXPECTED_STATE substitution ────────────
// These match the Bundle constant field names: Bundle.ACTIVE, Bundle.RESOLVED …
export type OsgiStateName = 'ACTIVE' | 'RESOLVED' | 'INSTALLED' | 'STARTING' | 'STOPPING';

// ─── Groovy-based start / stop ────────────────────────────────────────────────

/**
 * Start an already-installed (Resolved) bundle.
 * Fails the test immediately if the Groovy script yields '.failed'.
 */
export const startModule = (bundleKey: string, bundleVersion: string): Cypress.Chainable =>
    cy.executeGroovy('groovy/startModule.groovy', {BUNDLE_KEY: bundleKey, BUNDLE_VERSION: bundleVersion})
        .then((result: any) => assertGroovyOk(result, `startModule(${bundleKey})`));

/**
 * Stop a running bundle.
 * Fails the test immediately if the Groovy script yields '.failed'.
 */
export const stopModule = (bundleKey: string, bundleVersion: string): Cypress.Chainable =>
    cy.executeGroovy('groovy/stopModule.groovy', {BUNDLE_KEY: bundleKey, BUNDLE_VERSION: bundleVersion})
        .then((result: any) => assertGroovyOk(result, `stopModule(${bundleKey})`));

/**
 * Stop a bundle only if it is currently ACTIVE; does nothing if it is not found
 * or already in any other state.  Never fails the test – safe for use in teardown
 * hooks and setup guards.
 */
export const stopModuleSafe = (bundleKey: string, bundleVersion: string): Cypress.Chainable =>
    cy.executeGroovy('groovy/checkModuleState.groovy', {
        BUNDLE_KEY: bundleKey,
        BUNDLE_VERSION: bundleVersion,
        EXPECTED_STATE: 'ACTIVE'
    }).then((result: any) => {
        const status: string = Array.isArray(result) ? result[0] : String(result);
        if (status === '.installed') {
            return stopModule(bundleKey, bundleVersion);
        }
        // Already stopped, not found, or in a transient state – nothing to do.
    });

// ─── State polling ────────────────────────────────────────────────────────────

/**
 * Wait until a bundle reaches the expected OSGi state.
 *
 * Uses checkModuleState.groovy which throws (→ '.failed') when the state does
 * not match yet, and succeeds (→ '.installed') when it does.
 * cy.waitUntil polls until '.installed' is seen or the timeout is reached.
 *
 * @param bundleKey      Bundle symbolic name, e.g. 'schema-update-test-module'
 * @param bundleVersion  Bundle version,        e.g. '1.0.0-SNAPSHOT'
 * @param expectedState  OSGi Bundle constant name: 'ACTIVE' | 'RESOLVED' | 'INSTALLED'
 * @param timeoutMs      Maximum wait time in ms (default 30 000)
 */
export const waitForModuleState = (
    bundleKey: string,
    bundleVersion: string,
    expectedState: OsgiStateName,
    timeoutMs = 30000
): Cypress.Chainable =>
    cy.waitUntil(
        () =>
            cy.executeGroovy('groovy/checkModuleState.groovy', {
                BUNDLE_KEY: bundleKey,
                BUNDLE_VERSION: bundleVersion,
                EXPECTED_STATE: expectedState
            }).then((result: any) => {
                const status: string = Array.isArray(result) ? result[0] : String(result);
                console.log(`[waitForModuleState] ${bundleKey}@${bundleVersion} status='${status}' (waiting for '${expectedState}')`);
                return status === '.installed';
            }),
        {
            timeout: timeoutMs,
            interval: 2000,
            errorMsg: `Bundle ${bundleKey}/${bundleVersion} did not reach state '${expectedState}' within ${timeoutMs}ms`
        }
    );

// ─── Install / uninstall (REST – jar upload, rarely needed) ──────────────────

/**
 * Install a module by uploading a jar via the Jahia REST API.
 * Only needed when the module is not yet present on the server at all.
 * Prefer deploying modules via provisioning manifests in CI instead.
 */
export const installModuleFromFile = (jarFixturePath: string): Cypress.Chainable => {
    const JAHIA_BASE_URL = Cypress.config('baseUrl') || 'http://localhost:8080';
    const auth = `Basic ${btoa('root:' + (Cypress.env('SUPER_USER_PASSWORD') || 'root1234'))}`;
    return cy.fixture(jarFixturePath, 'binary').then(Cypress.Blob.binaryStringToBlob).then(blob => {
        const formData = new FormData();
        formData.append('bundle', blob, jarFixturePath.split('/').pop());
        return cy.request({
            method: 'POST',
            url: `${JAHIA_BASE_URL}/modules/api/bundles`,
            headers: {Authorization: auth},
            body: formData,
            failOnStatusCode: false
        });
    });
};

// ─── Karaf SSH helper (upgrade only) ─────────────────────────────────────────

/**
 * Upgrade an already-installed bundle in-place via Karaf SSH.
 * Uses bundle:update <id> file:<path> + bundle:refresh <id>.
 * Required for Scenario G (v1 → v2 upgrade) – no Groovy / REST equivalent.
 */
export const upgradeModuleViaKaraf = (bundleSymbolicName: string, newJarPathOnServer: string): Cypress.Chainable => {
    return cy.task('sshCommand', [
        `bundle:list -s | grep ${bundleSymbolicName}`
    ]).then((output: string) => {
        const match = output && output.match(/^\s*(\d+)\s*\|/m);
        if (!match) {
            throw new Error(`[upgradeModuleViaKaraf] Bundle ${bundleSymbolicName} not found – is v1 started?`);
        }

        const id = match[1];
        return cy.task('sshCommand', [
            `bundle:update ${id} file:${newJarPathOnServer}`,
            `bundle:refresh ${id}`
        ]);
    });
};
