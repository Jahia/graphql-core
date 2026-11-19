import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

/**
 * Writes the shipped bounds of the subscription endpoint (the graphql.websocket.* properties) into the default
 * GraphQL provider configuration (org.jahia.modules.graphql.provider~default).
 *
 * Runs on bundle start (the ".started" suffix) so it reaches existing installs too: the shipped default cfg is
 * not applied again to a module that is already installed.
 *
 * Like 04 and 05, this patch carries no value the code lacks: each bound is in force either way, since its
 * code default is the value below (see WebSocketLimits). What the patch adds is the properties themselves, so
 * that an operator of an upgraded instance finds them in the configuration file next to the other limits and
 * can tune them there. The values below therefore stay equal to the code defaults and to the shipped cfg.
 *
 * Idempotent and non-intrusive: a property that is already set to an explicit value, a deliberate 0 included,
 * is left untouched, and only an absent property is added.
 */
def setSubscriptionLimits() {
    def limits = [
            "graphql.websocket.initTimeout"                  : "10",
            "graphql.websocket.maxConnections"               : "1000",
            "graphql.websocket.maxConnectionsPerUser"        : "20",
            "graphql.websocket.maxSubscriptionsPerConnection": "50"
    ]

    def configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
    def config = configAdmin.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
    if (config == null) {
        log.warn("No default GraphQL provider configuration found; cannot write the subscription limits.")
        return
    }

    def props = config.getProperties()
    if (props == null) {
        props = new java.util.Hashtable()
    }
    def written = [:]
    limits.each { key, value ->
        def current = props.get(key)
        if (current == null || current.toString().trim().isEmpty()) {
            props.put(key, value)
            written[key] = value
        }
    }
    if (written.isEmpty()) {
        log.info("GraphQL subscription limits already configured; leaving as-is.")
        return
    }

    config.update(props)
    log.info("GraphQL subscription limits written to the default configuration (${written}): the properties can " +
            "now be tuned there. Set 0 to lift a bound.")
}

setSubscriptionLimits()
