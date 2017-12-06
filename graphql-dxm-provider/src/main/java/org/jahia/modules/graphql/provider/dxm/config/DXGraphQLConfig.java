package org.jahia.modules.graphql.provider.dxm.config;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * OSGI managed service that handle the graphql configuration file and load the different properties
 *
 * Created by kevan
 */
@Component(service = ManagedService.class, configurationPid = "org.jahia.modules.graphql.provider")
public class DXGraphQLConfig implements ManagedService {

    private final static String PERMISSION_PREFIX = "permission.";

    Map<String, Set<DXGraphQLConfigFieldPermission>> permissions = new HashMap<>();

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
            if (key.startsWith(PERMISSION_PREFIX)) {
                String[] splittedKey = StringUtils.split(key, ".");
                if(splittedKey.length >= 3 && StringUtils.isNotEmpty(splittedKey[1]) && StringUtils.isNotEmpty(splittedKey[2])) {

                    Set<DXGraphQLConfigFieldPermission> fieldPermissions;
                    if(!permissions.containsKey(splittedKey[1])) {
                        fieldPermissions = new HashSet<>();
                        permissions.put(splittedKey[1], fieldPermissions);
                    } else {
                        fieldPermissions = permissions.get(splittedKey[1]);
                    }

                    if (properties.get(key) != null) {
                        fieldPermissions.add(new DXGraphQLConfigFieldPermission(splittedKey[2], (String) properties.get(key)));
                    }
                }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DXGraphQLConfigFieldPermission that = (DXGraphQLConfigFieldPermission) o;
            return Objects.equals(field, that.field) &&
                    Objects.equals(permission, that.permission);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, permission);
        }
    }
}
