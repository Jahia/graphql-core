import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

/**
 * Applies the default GraphQL query-cost guards (graphql.query.maxComplexity=2000, graphql.query.maxDepth=30)
 * to the default GraphQL provider configuration (org.jahia.modules.graphql.provider~default), so expensive or
 * abusive queries are rejected before execution out of the box.
 *
 * Runs on bundle start (the ".started" suffix) so it reaches EXISTING installs too: the shipped default cfg is
 * NOT re-applied to an already-installed module, so these properties would otherwise stay absent (code default 0,
 * i.e. disabled) on any instance that already had graphql-dxm-provider. Writing through ConfigurationAdmin against
 * the existing "default" factory instance preserves its felix.fileinstall.filename, which the provider requires:
 * the two limits are only honoured when they originate from org.jahia.modules.graphql.provider-default.cfg, so that
 * a third-party module configuration cannot loosen them.
 *
 * Idempotent and non-intrusive: each property is only added when it is absent. An explicit administrator value
 * (in either direction, including 0 to opt out) is left untouched.
 */
def enableQueryCostGuards() {
    def defaults = ["graphql.query.maxComplexity": "2000", "graphql.query.maxDepth": "30"]

    def configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
    def config = configAdmin.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
    if (config == null) {
        log.warn("No default GraphQL provider configuration found; cannot apply the query-cost guard defaults.")
        return
    }

    def props = config.getProperties()
    if (props == null) {
        props = new java.util.Hashtable()
    }

    def added = [:]
    defaults.each { key, value ->
        def current = props.get(key)
        if (current != null && !current.toString().trim().isEmpty()) {
            log.info("GraphQL query-cost guard already configured (${key}=${current}); leaving as-is.")
        } else {
            props.put(key, value)
            added.put(key, value)
        }
    }

    if (added.isEmpty()) {
        return
    }

    config.update(props)
    log.info("GraphQL query-cost guards applied by default (${added}): queries exceeding these limits are now " +
            "rejected before execution. Set a property to 0 to disable that guard.")
}

enableQueryCostGuards()
