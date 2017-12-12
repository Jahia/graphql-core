/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.config;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * OSGI managed service that handle the graphql configuration file and load the different properties
 *
 * Created by kevan
 */
@Component(service = {DXGraphQLConfig.class, ManagedServiceFactory.class}, property = "service.pid=org.jahia.modules.graphql.provider", immediate = true)
public class DXGraphQLConfig implements ManagedServiceFactory {

    private final static String PERMISSION_PREFIX = "permission.";

    private Map<String, List<String>> keysByPid = new HashMap<>();
    private Map<String, String> permissions = new HashMap<>();

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
        // parse properties
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            keysForPid.add(key);
            // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
            if (key.startsWith(PERMISSION_PREFIX) && properties.get(key) != null) {
                permissions.put(key.substring(PERMISSION_PREFIX.length()), (String) properties.get(key));
            }
        }
    }

    @Override
    public void deleted(String pid) {
        List<String> keysForPid = keysByPid.get(pid);
        if (keysForPid != null) {
            for (String key : keysForPid) {
                // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
                if (key.startsWith(PERMISSION_PREFIX)) {
                    permissions.remove(key.substring(PERMISSION_PREFIX.length()));
                }
            }
            keysByPid.remove(pid);
        }
    }


    public Map<String, String> getPermissions() {
        return permissions;
    }
}
