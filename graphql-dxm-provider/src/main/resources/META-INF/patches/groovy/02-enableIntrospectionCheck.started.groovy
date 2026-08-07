import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

/**
 * Applies the recommended secure default for GraphQL schema introspection: enables the introspection
 * permission check (introspectionCheckEnabled=true) in the default GraphQL provider configuration
 * (org.jahia.modules.graphql.provider~default), so introspection queries are restricted to users
 * holding the developerToolsAccess permission.
 *
 * Runs on bundle start (the ".started" suffix) so it reaches EXISTING installs too — the shipped
 * default cfg is not re-applied to an already-installed module, so the flag would otherwise stay unset
 * (its code default is false) on any instance that already had graphql-dxm-provider. Writing through
 * ConfigurationAdmin persists to the deployed configuration (fileinstall writes it back to the .cfg).
 *
 * Idempotent and non-intrusive: if introspectionCheckEnabled is already set to an explicit value
 * (an administrator's choice, in either direction), it is left untouched; the flag is only added when
 * it is absent.
 */
def enableIntrospectionCheck() {
    def key = "introspectionCheckEnabled"
    def configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
    def config = configAdmin.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
    if (config == null) {
        log.warn("No default GraphQL provider configuration found; cannot apply the introspection check default.")
        return
    }

    def props = config.getProperties()
    def current = props?.get(key)
    if (current != null && !current.toString().trim().isEmpty()) {
        log.info("GraphQL introspection check already configured (${key}=${current}); leaving as-is.")
        return
    }

    if (props == null) {
        props = new java.util.Hashtable()
    }
    props.put(key, "true")
    config.update(props)
    log.info("GraphQL introspection check enabled by default (${key}=true): schema introspection now " +
            "requires the developerToolsAccess permission.")
}

enableIntrospectionCheck()
