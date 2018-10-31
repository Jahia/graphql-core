/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.config;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.customApi.ConfigUtil;
import org.jahia.modules.graphql.provider.dxm.customApi.CustomApi;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
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

    private Map<String, List<String>> keysByPid = new HashMap<>();
    private Map<String, String> permissions = new HashMap<>();

    private Set<String> corsOrigins = new HashSet<>();
    private Map<String, Set<String>> corsOriginByPid = new HashMap<>();

    private Map<String, CustomApi> customApis = new HashMap<>();

    private ComponentContext componentContext;

    @Override
    public String getName() {
        return "DX GraphQL configurations";
    }

    @Activate
    public void activate(ComponentContext context) {
        this.componentContext = context;
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        componentContext.disableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");

        if (properties == null) {
            return;
        }

        deleted(pid);

        ArrayList<String> keysForPid = new ArrayList<>();
        keysByPid.put(pid, keysForPid);
        corsOriginByPid.remove(pid);

        // parse properties
        Enumeration<String> keys = properties.keys();
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
                        logger.warn("Unable to register permission for {} because it has been already registered by the config with id {}", key, p);
                        break;
                    }
                }
                if (addKey) {
                    permissions.put(key.substring(PERMISSION_PREFIX.length()), value);
                    // store the key for the permission configuration
                    keysForPid.add(key);
                }
            } else if (key.equals(CORS_ORIGINS)) {
                corsOriginByPid.put(pid, new HashSet<>(Arrays.asList(StringUtils.split(value," ,"))));
            } else if (key.startsWith(TYPE_PREFIX)) {
                keysForPid.add(key);
                ConfigUtil.configureCustomApi(key.substring(TYPE_PREFIX.length()), value, customApis);
            } else {
                // store other properties than permission configuration
                keysForPid.add(key);
            }
        }
        componentContext.enableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
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


    public Map<String, String> getPermissions() {
        return permissions;
    }

    public Map<String, CustomApi> getCustomApis() {
        return customApis;
    }

    public Set<String> getCorsOrigins() {
        return corsOrigins;
    }
}
