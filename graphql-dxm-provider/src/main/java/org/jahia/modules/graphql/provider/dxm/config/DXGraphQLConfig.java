package org.jahia.modules.graphql.provider.dxm.config;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * Created by kevan
 */
@Component(service = ManagedService.class, configurationPid = "org.jahia.modules.graphql.provider")
public class DXGraphQLConfig implements ManagedService {

    private final static String PERMISSION_PREFIX = "permission.";

    Map<String, Set<DXGraphQLConfigFieldPermission>> permissions = null;

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

        if (properties == null) {
            return;
        }

        // init
        permissions = new HashMap<>();

        // parse properties
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(PERMISSION_PREFIX)) {
                // parse permission
                String[] splittedKey = StringUtils.split(key, ".");
                // todo
            }
        }
    }

    public Map<String, Set<DXGraphQLConfigFieldPermission>> getPermissions() {
        return permissions;
    }

    public class DXGraphQLConfigFieldPermission {
        private String field;
        private String permission;

        private DXGraphQLConfigFieldPermission(String field, String permission) {
            this.field = field;
            this.permission = permission;
        }

        public String getField() {
            return field;
        }

        public String getPermission() {
            return permission;
        }
    }
}
