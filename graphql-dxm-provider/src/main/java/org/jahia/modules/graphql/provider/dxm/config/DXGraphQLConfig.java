package org.jahia.modules.graphql.provider.dxm.config;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * OSGI managed service that handle the graphql configuration file and load the different properties
 *
 * Created by kevan
 */
@Component(service = {DXGraphQLConfig.class, ManagedService.class}, configurationPid = "org.jahia.modules.graphql.provider", immediate = true)
public class DXGraphQLConfig implements ManagedService {

    private final static String PERMISSION_PREFIX = "permission.";

    private Map<String, String> permissions = new HashMap<>();

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

        if (properties == null) {
            return;
        }

        // parse properties
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            // parse permissions ( permission format is like: permission.Query.nodesByQuery = privileged )
            if (key.startsWith(PERMISSION_PREFIX) && properties.get(key) != null) {
                permissions.put(key.substring(PERMISSION_PREFIX.length()), (String) properties.get(key));
            }
        }
    }

    public Map<String, String> getPermissions() {
        return permissions;
    }
}
