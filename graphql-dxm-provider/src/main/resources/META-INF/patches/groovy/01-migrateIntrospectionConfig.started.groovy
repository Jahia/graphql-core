import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

/**
 * Migrates the GraphQL introspection configuration from the old 8.1 property name to the new one.
 * Looks for 'graphql.introspection.enabled' in the default GraphQL provider configuration
 * (org.jahia.modules.graphql.provider~default) and replaces it with 'introspectionCheckEnabled',
 * inverting the boolean value (If introspection has been disabled in 8.1 (false), then we enable the introspection check).
 * Removes the old property after migration.
 */
def migrateIntrospectionConfig() {

    def configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);

    def config = configAdmin.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null);

    if (config == null) {
        log.warn("No default GraphQL provider configuration found.");
        return;
    }
    log.info("Found default GraphQL provider configuration with PID: '${config.getPid()}'");

    def props = config.getProperties();
    if (props == null || props.isEmpty()) {
        log.info("No properties found in default GraphQL provider configuration (may not exist yet).");
        return;
    }

    def oldKey = "graphql.introspection.enabled";
    def newKey = "introspectionCheckEnabled";

    if (props.get(oldKey) != null) {
        def oldValue = props.get(oldKey);

        if (oldValue != null && !oldValue.toString().trim().isEmpty()) {
            // We invert the boolean value here so that if introspection has been disabled in 8.1,
            // Then this means that we want introspection in 8.2 to be allowed only through permissions.
            def newValue;
            if (oldValue instanceof Boolean) {
                newValue = !oldValue;
            } else {
                def oldValueStr = oldValue.toString().toLowerCase();
                newValue = !(oldValueStr == "true");
            }

            log.info("Migrating introspection config: ${oldKey}=${oldValue} to ${newKey}=${newValue} (inverted)");
            props.put(newKey, newValue);
            log.info("GraphQL introspection configuration successfully migrated.");
        } else {
            log.info("Property ${oldKey} found but value is empty. Removing it.");
        }
        props.remove(oldKey);
        config.update(props);
    } else {
        log.info("No ${oldKey} property found, migration not needed.");
    }
}

migrateIntrospectionConfig()
