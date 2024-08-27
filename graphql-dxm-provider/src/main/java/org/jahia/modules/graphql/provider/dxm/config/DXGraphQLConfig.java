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
import org.jahia.settings.SettingsBean;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OSGI managed service that handle the graphql configuration file and load the different properties
 *
 * Created by kevan
 */
@Component(service = {DXGraphQLConfig.class, ManagedServiceFactory.class}, property = "service.pid=org.jahia.modules.graphql.provider", immediate = true)
public class DXGraphQLConfig implements ManagedServiceFactory {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLConfig.class);
    private final static String PERMISSION_PREFIX = "permission.";
    private final static String TYPE_PREFIX = "type.";

    private final static String CORS_ORIGINS = "http.cors.allow-origin";
    private final static String NODE_LIMIT = "graphql.fields.node.limit";

    private final static String ENABLE_INTROSPECTION_MODE = "graphql.introspection.enabled";

    private Map<String, List<String>> keysByPid = new HashMap<>();
    private Map<String, String> permissions = new HashMap<>();

    private Set<String> corsOrigins = new HashSet<>();
    private Map<String, Set<String>> corsOriginByPid = new HashMap<>();

    private int nodeLimit = 5000;
    private boolean introspectionEnabled = false;

    private ComponentContext componentContext;

    @Override
    public String getName() {
        return "DX GraphQL configurations";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            return;
        }

        deleted(pid);

        ArrayList<String> keysForPid = new ArrayList<>();
        keysByPid.put(pid, keysForPid);
        corsOriginByPid.remove(pid);

        // parse properties
        Enumeration<String> keys = properties.keys();
        boolean isDefaultConfig = properties.get("felix.fileinstall.filename") != null &&
                properties.get("felix.fileinstall.filename").toString().endsWith("org.jahia.modules.graphql.provider-default.cfg");

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            boolean addKey = true;
            // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
            String value = (String) properties.get(key);
            if (key.startsWith(PERMISSION_PREFIX) && value != null) {
                // check if any configuration also contains the same permission configuration
                for (String p : keysByPid.keySet()) {
                    if (!StringUtils.equals(p, pid) && keysByPid.get(p).contains(key)) {
                        addKey = false;
                        logger.warn("Unable to register permission for {} because it has been already registered by the config with id {}",
                                key, p);
                        break;
                    }
                }
                if (addKey) {
                    permissions.put(key.substring(PERMISSION_PREFIX.length()), value);
                    // store the key for the permission configuration
                    keysForPid.add(key);
                }
            } else if (key.equals(CORS_ORIGINS)) {
                corsOriginByPid.put(pid, new HashSet<>(Arrays.asList(StringUtils.split(value, " ,"))));
            } else if (isDefaultConfig && key.equals(NODE_LIMIT)) {
                try {
                    int newNodeLimit = Integer.parseInt(value);
                    if (newNodeLimit < 0) {
                        throw new ConfigurationException(key, "Node limit must be a positive integer");
                    }
                    if (newNodeLimit != nodeLimit) {
                        logger.info("Node limit has been updated to {} by pid {} (file/config) {}.cfg", newNodeLimit, pid, pid);
                        nodeLimit = newNodeLimit;
                        PaginationHelper.updateLimit(nodeLimit);
                    }
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(key, "Node limit must be a positive integer");
                }
            } else if (isDefaultConfig && key.equals(ENABLE_INTROSPECTION_MODE)) {
                introspectionEnabled = Boolean.parseBoolean(value);
            } else {
                // store other properties than permission configuration
                keysForPid.add(key);
            }
        }
        corsOrigins = corsOriginByPid.keySet().stream().flatMap(k -> corsOriginByPid.get(k).stream()).collect(Collectors.toSet());
    }

    @Override
    public void deleted(String pid) {
        List<String> keysForPid = keysByPid.get(pid);
        if (keysForPid != null) {
            for (String key : keysForPid) {
                // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
                if (key.startsWith(PERMISSION_PREFIX)) {
                    permissions.remove(key.substring(PERMISSION_PREFIX.length()));
                } else if (key.equals(CORS_ORIGINS)) {
                    corsOriginByPid.remove(pid);
                    corsOrigins = corsOriginByPid.keySet().stream().flatMap(k -> corsOriginByPid.get(k).stream()).collect(Collectors.toSet());
                }
            }
            keysByPid.remove(pid);
        }
    }

    public boolean isIntrospectionEnabled() {
        return SettingsBean.getInstance().isDevelopmentMode() || introspectionEnabled;
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
}
