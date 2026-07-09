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

    private static final int DEFAULT_NODE_LIMIT = 5000;

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
    public void shouldApplyLimitsFromDefaultConfigFile() throws ConfigurationException {
        config.updated("pid1", props(DEFAULT_CONFIG_FILE,
                "graphql.query.maxComplexity", "2000",
                "graphql.query.maxDepth", "30",
                "graphql.fields.node.limit", "100"));

        assertEquals(2000, config.getMaxQueryComplexity());
        assertEquals(30, config.getMaxQueryDepth());
        assertEquals(100, config.getNodeLimit());
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
