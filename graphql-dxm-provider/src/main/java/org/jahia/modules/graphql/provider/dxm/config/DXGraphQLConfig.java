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
package org.jahia.modules.graphql.provider.dxm.config;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OSGI managed service that handle the graphql configuration file and load the different properties
 *
 * Created by kevan
 */
@Component(
        service = {DXGraphQLConfig.class, ManagedServiceFactory.class},
        property = "service.pid=org.jahia.modules.graphql.provider",
        immediate = true
)
public class DXGraphQLConfig implements ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(DXGraphQLConfig.class);
    private static final String PERMISSION_PREFIX = "permission.";
    private static final String TYPE_PREFIX = "type.";

    private static final String CORS_ORIGINS = "http.cors.allow-origin";
    private static final String NODE_LIMIT = "graphql.fields.node.limit";
    private static final String MAX_QUERY_COMPLEXITY = "graphql.query.maxComplexity";
    private static final String MAX_QUERY_DEPTH = "graphql.query.maxDepth";
    public static final String INTROSPECTION_CHECK_ENABLED = "introspectionCheckEnabled";

    private static final String DEFAULT_CONFIG_FILE_SUFFIX = "org.jahia.modules.graphql.provider-default.cfg";

    private static final int DEFAULT_NODE_LIMIT = 5000;

    private final Map<String, List<String>> keysByPid = new ConcurrentHashMap<>();
    private final Map<String, String> annotationPermissions = new ConcurrentHashMap<>();
    private final Map<String, String> configPermissions = new ConcurrentHashMap<>();
    private volatile Map<String, String> permissions = Collections.emptyMap();

    private volatile Set<String> corsOrigins = new HashSet<>();
    private final  Map<String, Set<String>> corsOriginByPid = new ConcurrentHashMap<>();
    private final Map<String, Boolean> introspectionCheckByPid = new ConcurrentHashMap<>();
    // Global limits are only accepted from the default config file. We track the source value per pid so that when
    // that source stops providing a value (property removed, or config deleted) the effective value reverts to the
    // code default instead of sticking at the last value that was ever set.
    private final Map<String, Integer> nodeLimitByPid = new ConcurrentHashMap<>();
    private final Map<String, Integer> maxQueryComplexityByPid = new ConcurrentHashMap<>();
    private final Map<String, Integer> maxQueryDepthByPid = new ConcurrentHashMap<>();

    private volatile int nodeLimit = DEFAULT_NODE_LIMIT;
    private volatile int maxQueryComplexity = 0;
    private volatile int maxQueryDepth = 0;
    // Secure by default: the introspection permission check is on unless a configuration explicitly disables it.
    private volatile boolean introspectionCheckEnabled = true;

    @Override
    public String getName() {
        return "DX GraphQL configurations";
    }

    @Override
    public synchronized void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            return;
        }

        // Some limits (node limit, query-cost guards) are global and may only be set from the default config file,
        // so that a third-party module configuration cannot loosen them.
        Object filename = properties.get("felix.fileinstall.filename");
        boolean isDefaultConfig = filename != null && filename.toString().endsWith(DEFAULT_CONFIG_FILE_SUFFIX);

        // Parse everything into locals first. A validation failure throws HERE, before any shared state is mutated,
        // so a rejected configuration leaves the last-known-good state intact instead of half-applied. Only once the
        // whole document parses successfully do we swap in the new state and recompute (the apply block never throws).
        List<String> newKeys = new ArrayList<>();
        Map<String, String> newPermissions = new HashMap<>();
        Set<String> newCorsOrigins = null;
        Integer newNodeLimit = null;
        Integer newMaxQueryComplexity = null;
        Integer newMaxQueryDepth = null;
        Boolean newIntrospectionCheck = null;

        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
            String value = (String) properties.get(key);
            if (key.startsWith(PERMISSION_PREFIX) && value != null) {
                // check if any OTHER configuration also contains the same permission configuration
                boolean alreadyRegistered = false;
                for (Map.Entry<String, List<String>> entry : keysByPid.entrySet()) {
                    if (!StringUtils.equals(entry.getKey(), pid) && entry.getValue().contains(key)) {
                        alreadyRegistered = true;
                        logger.warn("Unable to register permission for {} because it has been already registered by the config with id {}", key, entry.getKey());
                        break;
                    }
                }
                if (!alreadyRegistered) {
                    newPermissions.put(key.substring(PERMISSION_PREFIX.length()), value);
                    // store the key for the permission configuration
                    newKeys.add(key);
                }
            } else if (key.equals(CORS_ORIGINS)) {
                newCorsOrigins = new HashSet<>(Arrays.asList(StringUtils.split(value," ,")));
            } else if (key.equals(NODE_LIMIT)) {
                if (isDefaultConfig) {
                    newNodeLimit = parseNonNegativeLimit(key, value, "Node limit");
                } else {
                    warnGatedPropertyIgnored(key, pid);
                }
            } else if (key.equals(MAX_QUERY_COMPLEXITY)) {
                if (isDefaultConfig) {
                    newMaxQueryComplexity = parseNonNegativeLimit(key, value, "Max query complexity");
                } else {
                    warnGatedPropertyIgnored(key, pid);
                }
            } else if (key.equals(MAX_QUERY_DEPTH)) {
                if (isDefaultConfig) {
                    newMaxQueryDepth = parseNonNegativeLimit(key, value, "Max query depth");
                } else {
                    warnGatedPropertyIgnored(key, pid);
                }
            } else if (key.equals(INTROSPECTION_CHECK_ENABLED)) {
                // Unlike the limits above this is intentionally NOT restricted to the default config file: any config
                // may contribute, but the "true wins" aggregation (see recomputeConfig) means a configuration can only
                // tighten the check (force it on), never loosen it, so allowing multiple sources is safe.
                newIntrospectionCheck = parseIntrospectionCheckEnabled(key, value, pid);
            } else {
                // store other properties than permission configuration
                newKeys.add(key);
            }
        }

        // Parse succeeded: apply atomically. Nothing below throws.
        clearConfigForPid(pid);
        keysByPid.put(pid, newKeys);
        newPermissions.forEach(configPermissions::put);
        if (newCorsOrigins != null) {
            corsOriginByPid.put(pid, newCorsOrigins);
        }
        if (newNodeLimit != null) {
            nodeLimitByPid.put(pid, newNodeLimit);
        }
        if (newMaxQueryComplexity != null) {
            maxQueryComplexityByPid.put(pid, newMaxQueryComplexity);
        }
        if (newMaxQueryDepth != null) {
            maxQueryDepthByPid.put(pid, newMaxQueryDepth);
        }
        if (newIntrospectionCheck != null) {
            introspectionCheckByPid.put(pid, newIntrospectionCheck);
        }
        recomputeConfig();
    }

    @Override
    public synchronized void deleted(String pid) {
        clearConfigForPid(pid);
        recomputeConfig();
    }

    /**
     * Removes all state contributed by the given configuration pid. Does not recompute the effective values;
     * callers pair this with {@link #recomputeConfig()} so a single recompute covers both add and remove.
     */
    private void clearConfigForPid(String pid) {
        List<String> keysForPid = keysByPid.remove(pid);
        if (keysForPid != null) {
            for (String key : keysForPid) {
                if (key.startsWith(PERMISSION_PREFIX)) {
                    configPermissions.remove(key.substring(PERMISSION_PREFIX.length()));
                }
            }
        }
        corsOriginByPid.remove(pid);
        introspectionCheckByPid.remove(pid);
        nodeLimitByPid.remove(pid);
        maxQueryComplexityByPid.remove(pid);
        maxQueryDepthByPid.remove(pid);
    }

    /**
     * Recomputes every effective value from the current per-pid state. Global limits fall back to their code default
     * when no configuration provides them, so removing a property (or deleting the default config) reverts the limit
     * instead of keeping the last value that was ever set.
     */
    private void recomputeConfig() {
        corsOrigins = corsOriginByPid.keySet().stream().flatMap(k -> corsOriginByPid.get(k).stream()).collect(Collectors.toSet());
        // Secure by default: enabled unless a configuration is present and every configuration disables it. With no
        // configuration at all the check stays on, and a single config that sets true keeps it on ("true wins").
        introspectionCheckEnabled = introspectionCheckByPid.isEmpty()
                || introspectionCheckByPid.values().stream().anyMatch(Boolean::booleanValue);

        int newNodeLimit = firstValueOrDefault(nodeLimitByPid, DEFAULT_NODE_LIMIT);
        if (newNodeLimit != nodeLimit) {
            nodeLimit = newNodeLimit;
            PaginationHelper.updateLimit(nodeLimit);
        }
        maxQueryComplexity = firstValueOrDefault(maxQueryComplexityByPid, 0);
        maxQueryDepth = firstValueOrDefault(maxQueryDepthByPid, 0);

        rebuildPermissions();
    }

    private static int firstValueOrDefault(Map<String, Integer> valuesByPid, int defaultValue) {
        // These limits are only accepted from the default config file, which is a single source: normally at most one
        // entry. Should more than one ever coexist, pick by lowest pid so the result is deterministic regardless of
        // map iteration order. (We intentionally do NOT reduce by min value: for the guards, 0 means "disabled", so a
        // min-by-value could silently turn a guard off.)
        return valuesByPid.entrySet().stream()
                .min(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(defaultValue);
    }

    /**
     * Parses the introspection permission-check flag. This is a security toggle whose secure state is {@code true},
     * so an empty or unrecognized value defaults to {@code true} (and is logged) rather than silently disabling the
     * check. Only an explicit "false" disables it.
     */
    private static boolean parseIntrospectionCheckEnabled(String key, String value, String pid) {
        String trimmed = StringUtils.trimToEmpty(value);
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        logger.warn("Invalid value '{}' for '{}' in configuration '{}'; defaulting to enabled (the secure setting). " +
                "Use 'true' or 'false'.", value, key, pid);
        return true;
    }

    private static void warnGatedPropertyIgnored(String key, String pid) {
        logger.warn("Ignoring GraphQL limit '{}' set by configuration '{}': this limit is only honored from the " +
                "default configuration file (a file whose name ends with {}), so that a non-default configuration " +
                "cannot change it.", key, pid, DEFAULT_CONFIG_FILE_SUFFIX);
    }

    public void addAnnotationPermission(String pid, String permission) {
        annotationPermissions.put(pid, permission);
        rebuildPermissions();
    }

    public void clearAnnoationPermissions() {
        annotationPermissions.clear();
        rebuildPermissions();
    }


    public Map<String, String> getPermissions() {
        return permissions;
    }

    public Set<String> getCorsOrigins() {
        return corsOrigins;
    }

    public int getNodeLimit() {
        return nodeLimit;
    }

    /**
     * @return the maximum allowed query complexity (estimated resolver cost), or 0 when the guard is disabled
     */
    public int getMaxQueryComplexity() {
        return maxQueryComplexity;
    }

    /**
     * @return the maximum allowed query depth, or 0 when the guard is disabled
     */
    public int getMaxQueryDepth() {
        return maxQueryDepth;
    }

    private static int parseNonNegativeLimit(String key, String value, String label) throws ConfigurationException {
        try {
            int parsed = Integer.parseInt(StringUtils.trim(value));
            if (parsed < 0) {
                throw new ConfigurationException(key, label + " must be a non-negative integer");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ConfigurationException(key, label + " must be a non-negative integer");
        }
    }

    /**
     * @return true if introspection check is enabled in any configuration, false otherwise
     */
    public boolean isIntrospectionCheckEnabled() {
        return introspectionCheckEnabled;
    }

    private synchronized void  rebuildPermissions() {
        Map<String, String> merged = new HashMap<>(configPermissions);
        merged.putAll(annotationPermissions);
        permissions = Collections.unmodifiableMap(merged);
    }
}
