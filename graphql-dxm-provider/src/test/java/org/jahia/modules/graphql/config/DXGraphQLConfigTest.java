/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.config;

import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link DXGraphQLConfig}, focused on the global limits (node limit and query-cost guards):
 * <ul>
 *     <li>they are only honored when they come from the default configuration file;</li>
 *     <li>they revert to their code default when the property is removed or the configuration is deleted
 *     (regression guard against the "value sticks at the last set value" bug).</li>
 * </ul>
 */
public class DXGraphQLConfigTest {

    private static final String DEFAULT_CONFIG_FILE = "file:/opt/jahia/etc/org.jahia.modules.graphql.provider-default.cfg";
    private static final String OTHER_CONFIG_FILE = "file:/opt/jahia/etc/org.jahia.modules.graphql.provider-custom.cfg";

    private static final String DEFAULT_CONFIG_PID = "org.jahia.modules.graphql.provider~default";

    private static final int DEFAULT_NODE_LIMIT = 5000;
    private static final int DEFAULT_REQUEST_NODE_LIMIT = 20000;
    private static final int DEFAULT_OPERATION_LIMIT = 20;
    private static final int DEFAULT_MAX_EXPANDED_FIELDS = 2000;

    private DXGraphQLConfig config;

    @Before
    public void setUp() {
        config = new DXGraphQLConfig();
    }

    private static Dictionary<String, Object> props(String fileName, String... keyValues) {
        Dictionary<String, Object> dict = new Hashtable<>();
        if (fileName != null) {
            dict.put("felix.fileinstall.filename", fileName);
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            dict.put(keyValues[i], keyValues[i + 1]);
        }
        return dict;
    }

    @Test
    public void shouldDefaultToDisabledGuardsAndDefaultNodeLimit() {
        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(0, config.getMaxQueryDepth());
        assertEquals(DEFAULT_NODE_LIMIT, config.getNodeLimit());
    }

    @Test
    public void shouldApplyLimitsFromDefaultFactoryInstanceWithoutFileName() throws ConfigurationException {
        // Regression guard: an update written through ConfigurationAdmin against the default factory instance (what
        // the install-time groovy patcher does to seed the guards on existing installs) carries no
        // felix.fileinstall.filename. Keying the gate on the filename alone made such an update not merely ignored
        // but destructive -- the limits reverted to their code default, silently switching the guards off.
        config.updated(DEFAULT_CONFIG_PID, props(null,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30",
                "graphql.fields.node.limit", "100"));

        assertEquals(2000, config.getMaxQueryComplexity());
        assertEquals(30, config.getMaxQueryDepth());
        assertEquals(100, config.getNodeLimit());
    }

    // --- the default-configuration gate matches exactly, so no other configuration can claim to be it ---

    @Test
    public void shouldIgnoreLimitsFromPidMerelyEndingInDefaultSuffix() throws ConfigurationException {
        // Configuration Admin puts no constraint on '~' inside a factory instance name, so any bundle can create
        // "...provider~anything~default". Were the gate a suffix match, that instance would pass it -- and win over the
        // real one, since the effective value is picked by lowest pid.
        config.updated("org.jahia.modules.graphql.provider~aaa~default", props(null,
                "graphql.query.maxComplexity", "999999",
                "graphql.fields.node.limit", "1000000"));

        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(DEFAULT_NODE_LIMIT, config.getNodeLimit());
    }

    @Test
    public void shouldIgnoreLimitsFromFileNameMerelyEndingInDefaultFileName() throws ConfigurationException {
        // fileinstall derives the factory pid from the part before the first '-', so this file does reach this factory
        // while its name ends with the default configuration's name.
        config.updated("pid1", props(
                "file:/opt/jahia/etc/org.jahia.modules.graphql.provider-x.org.jahia.modules.graphql.provider-default.cfg",
                "graphql.query.maxComplexity", "999999"));

        assertEquals(0, config.getMaxQueryComplexity());
    }

    @Test
    public void shouldApplyLimitsFromDefaultConfigFile() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30",
                "graphql.fields.node.limit", "100"));

        assertEquals(2000, config.getMaxQueryComplexity());
        assertEquals(30, config.getMaxQueryDepth());
        assertEquals(100, config.getNodeLimit());
    }

    // --- the per-request node allowance, which unlike the guards above is on by default ---

    @Test
    public void shouldBoundNodesPerRequestByDefault() {
        // The allowance is what bounds nested fan-out, so it has to hold with no configuration present at all: unlike
        // the complexity and depth guards, whose code default is 0, an absent property must not leave it unbounded.
        assertEquals(DEFAULT_REQUEST_NODE_LIMIT, config.getRequestNodeLimit());
    }

    @Test
    public void shouldApplyRequestNodeLimitFromDefaultConfig() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.fields.node.requestLimit", "250"));

        assertEquals(250, config.getRequestNodeLimit());
    }

    @Test
    public void shouldIgnoreRequestNodeLimitFromNonDefaultConfig() throws ConfigurationException {
        config.updated("pid1", props(OTHER_CONFIG_FILE, "graphql.fields.node.requestLimit", "1000000"));

        assertEquals(DEFAULT_REQUEST_NODE_LIMIT, config.getRequestNodeLimit());
    }

    @Test
    public void shouldRevertRequestNodeLimitWhenPropertyRemoved() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.fields.node.requestLimit", "250"));
        assertEquals(250, config.getRequestNodeLimit());

        config.updated("pid1", props(DEFAULT_CONFIG_FILE));

        assertEquals(DEFAULT_REQUEST_NODE_LIMIT, config.getRequestNodeLimit());
    }

    @Test
    public void shouldTreatZeroRequestNodeLimitAsUnbounded() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.fields.node.requestLimit", "0"));

        assertEquals(0, config.getRequestNodeLimit());
    }

    @Test
    public void shouldTreatZeroNodeLimitAsUnbounded() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.fields.node.limit", "0"));

        assertEquals(0, config.getNodeLimit());
    }

    @Test
    public void shouldIgnoreLimitsFromNonDefaultConfigFile() throws ConfigurationException {
        config.updated("pid1", props(OTHER_CONFIG_FILE,
                "graphql.query.maxComplexity", "10",
                "graphql.query.maxDepth", "3",
                "graphql.fields.node.limit", "50"));

        // A non-default configuration cannot set (or loosen) these global limits.
        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(0, config.getMaxQueryDepth());
        assertEquals(DEFAULT_NODE_LIMIT, config.getNodeLimit());
    }

    @Test
    public void shouldIgnoreLimitsWhenNoSourceFileName() throws ConfigurationException {
        // Configurations created programmatically (e.g. ConfigurationAdmin.createFactoryConfiguration) have no
        // felix.fileinstall.filename, so they must not be able to set these limits.
        config.updated("pid1", props(null,
                "graphql.query.maxComplexity", "10",
                "graphql.query.maxDepth", "3"));

        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(0, config.getMaxQueryDepth());
    }

    @Test
    public void shouldRevertToDefaultsWhenPropertiesRemoved() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30",
                "graphql.fields.node.limit", "100"));
        assertEquals(2000, config.getMaxQueryComplexity());

        // Same pid updated again, this time without the limit properties -> must revert, not keep the old values.
        config.updated("pid1", props(DEFAULT_CONFIG_FILE));

        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(0, config.getMaxQueryDepth());
        assertEquals(DEFAULT_NODE_LIMIT, config.getNodeLimit());
    }

    @Test
    public void shouldRevertToDefaultsWhenConfigDeleted() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30"));
        assertEquals(2000, config.getMaxQueryComplexity());

        config.deleted("pid1");

        assertEquals(0, config.getMaxQueryComplexity());
        assertEquals(0, config.getMaxQueryDepth());
        assertEquals(DEFAULT_NODE_LIMIT, config.getNodeLimit());
    }

    @Test
    public void shouldTreatZeroAsDisabledGuard() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000"));
        assertEquals(2000, config.getMaxQueryComplexity());

        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "0"));
        assertEquals(0, config.getMaxQueryComplexity());
    }

    @Test
    public void shouldRejectNegativeLimit() {
        try {
            config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxComplexity", "-1"));
            fail("Expected a ConfigurationException for a negative limit");
        } catch (ConfigurationException e) {
            assertEquals("graphql.query.maxComplexity", e.getProperty());
        }
    }

    @Test
    public void shouldRejectNonNumericLimit() {
        try {
            config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxDepth", "abc"));
            fail("Expected a ConfigurationException for a non-numeric limit");
        } catch (ConfigurationException e) {
            assertEquals("graphql.query.maxDepth", e.getProperty());
        }
    }

    // --- updated() must be atomic: a rejected reload must not corrupt previously-good state ---

    @Test
    public void badReloadMustNotRevertPreviouslyGoodLimits() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30",
                "graphql.fields.node.limit", "100"));

        // Reload the same config with one bad value: the update must be rejected as a whole...
        try {
            config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                    "graphql.query.maxComplexity", "not-a-number",
                    "graphql.query.maxDepth", "30",
                    "graphql.fields.node.limit", "100"));
            fail("Expected a ConfigurationException for the invalid value");
        } catch (ConfigurationException expected) {
            // expected
        }

        // ...and the previously-good values must still be in effect right after the failed update.
        assertEquals(2000, config.getMaxQueryComplexity());
        assertEquals(30, config.getMaxQueryDepth());
        assertEquals(100, config.getNodeLimit());

        // A later unrelated config event triggers a recompute: values must NOT silently revert to defaults.
        config.deleted("some-other-pid");
        assertEquals(2000, config.getMaxQueryComplexity());
        assertEquals(30, config.getMaxQueryDepth());
        assertEquals(100, config.getNodeLimit());
    }

    @Test
    public void badReloadMustNotDropPreviouslyGoodPermission() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "permission.Query.foo", "myPermission",
                "graphql.query.maxComplexity", "2000"));
        assertEquals("myPermission", config.getPermissions().get("Query.foo"));

        try {
            config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                    "permission.Query.foo", "myPermission",
                    "graphql.query.maxComplexity", "boom"));
            fail("Expected a ConfigurationException for the invalid value");
        } catch (ConfigurationException expected) {
            // expected
        }

        // The permission from the last good load must survive the rejected reload.
        assertEquals("myPermission", config.getPermissions().get("Query.foo"));
    }

    @Test
    public void firstLoadRejectionMustNotActivatePartialState() throws ConfigurationException {
        // A brand-new config that fails validation must not leave any of its parsed-so-far values in effect,
        // even after a later recompute.
        try {
            config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                    "graphql.query.maxDepth", "15",
                    "graphql.query.maxComplexity", "nope"));
            fail("Expected a ConfigurationException for the invalid value");
        } catch (ConfigurationException expected) {
            // expected
        }

        config.deleted("some-other-pid"); // force a recompute
        assertEquals(0, config.getMaxQueryDepth());
        assertEquals(0, config.getMaxQueryComplexity());
    }

    // --- the bound on how many operations one request may submit, on by default like the allowance above ---

    @Test
    public void shouldBoundOperationsPerRequestByDefault() {
        // This bound is the factor the per-operation limits are multiplied by, so an absent property must not leave it
        // unbounded either.
        assertEquals(DEFAULT_OPERATION_LIMIT, config.getRequestOperationLimit());
    }

    @Test
    public void shouldApplyRequestOperationLimitFromDefaultConfig() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.request.operationLimit", "3"));

        assertEquals(3, config.getRequestOperationLimit());
    }

    @Test
    public void shouldIgnoreRequestOperationLimitFromOtherConfig() throws ConfigurationException {
        config.updated("pid1", props(OTHER_CONFIG_FILE, "graphql.request.operationLimit", "100000"));

        assertEquals(DEFAULT_OPERATION_LIMIT, config.getRequestOperationLimit());
    }

    @Test
    public void shouldRevertRequestOperationLimitWhenDefaultConfigDeleted() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.request.operationLimit", "3"));
        assertEquals(3, config.getRequestOperationLimit());

        config.deleted("pid1");
        assertEquals(DEFAULT_OPERATION_LIMIT, config.getRequestOperationLimit());
    }

    // --- the bound on how many fields one operation executes once its fragments are expanded, on by default ---

    @Test
    public void shouldBoundExpandedFieldsByDefault() {
        // Unlike the complexity and depth guards, whose code default is 0, this one is in force with no configuration
        // present, so an installation whose configuration names no such property is bounded all the same.
        assertEquals(DEFAULT_MAX_EXPANDED_FIELDS, config.getMaxExpandedFields());
    }

    @Test
    public void shouldApplyMaxExpandedFieldsFromDefaultConfig() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxExpandedFields", "300"));

        assertEquals(300, config.getMaxExpandedFields());
    }

    @Test
    public void shouldLiftExpandedFieldBoundWhenSetToZero() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxExpandedFields", "0"));

        assertEquals(0, config.getMaxExpandedFields());
    }

    @Test
    public void shouldIgnoreMaxExpandedFieldsFromOtherConfig() throws ConfigurationException {
        config.updated("pid1", props(OTHER_CONFIG_FILE, "graphql.query.maxExpandedFields", "100000"));

        assertEquals(DEFAULT_MAX_EXPANDED_FIELDS, config.getMaxExpandedFields());
    }

    @Test
    public void shouldRevertMaxExpandedFieldsWhenDefaultConfigDeleted() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxExpandedFields", "300"));
        assertEquals(300, config.getMaxExpandedFields());

        config.deleted("pid1");
        assertEquals(DEFAULT_MAX_EXPANDED_FIELDS, config.getMaxExpandedFields());
    }

    // --- introspection check flag: secure by default (true) ---

    @Test
    public void shouldEnableIntrospectionCheckByDefault() {
        // No configuration processed yet: the secure default is enabled.
        assertTrue(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldEnableIntrospectionCheckWhenNoConfigProvidesTheFlag() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "graphql.query.maxComplexity", "2000"));
        assertTrue(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldDisableIntrospectionCheckOnlyOnExplicitFalse() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "introspectionCheckEnabled", "false"));
        assertFalse(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldEnableIntrospectionCheckForExplicitTrue() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "introspectionCheckEnabled", "true"));
        assertTrue(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldDefaultIntrospectionCheckToEnabledForInvalidValue() throws ConfigurationException {
        // A typo must not silently open introspection: an unrecognized value falls back to the secure setting.
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "introspectionCheckEnabled", "treu"));
        assertTrue(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldKeepIntrospectionCheckEnabledWhenAnyConfigEnablesIt() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "introspectionCheckEnabled", "false"));
        config.updated("pid2", props(OTHER_CONFIG_FILE, "introspectionCheckEnabled", "true"));
        // "true wins" across configurations.
        assertTrue(config.isIntrospectionCheckEnabled());
    }

    @Test
    public void shouldRevertIntrospectionCheckToEnabledWhenDisablingConfigDeleted() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE, "introspectionCheckEnabled", "false"));
        assertFalse(config.isIntrospectionCheckEnabled());

        config.deleted("pid1");
        // No configuration disables it anymore -> back to the secure default.
        assertTrue(config.isIntrospectionCheckEnabled());
    }
}
