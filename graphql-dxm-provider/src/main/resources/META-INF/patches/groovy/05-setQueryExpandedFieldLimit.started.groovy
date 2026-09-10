import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

/**
 * Writes the shipped bound on how many fields one operation may execute once every fragment is expanded
 * at each place it is spread (graphql.query.maxExpandedFields=2000) into the default GraphQL provider
 * configuration (org.jahia.modules.graphql.provider~default).
 *
 * Runs on bundle start (the ".started" suffix) so it reaches EXISTING installs too — the shipped default
 * cfg is not re-applied to an already-installed module, so the property would otherwise be absent from the
 * configuration of any instance that already had graphql-dxm-provider. Writing through ConfigurationAdmin
 * persists to the deployed configuration (fileinstall writes it back to the .cfg).
 *
 * Like 04, and unlike 02 and 03, this patch carries no value the code lacks: the bound is in force either
 * way, since its code default IS the value below (see QueryCostInstrumentation), which is what applies when
 * no configuration names the property. What the patch adds is the property itself, so that an operator of
 * an upgraded instance finds it in the configuration file next to graphql.query.maxComplexity and
 * graphql.query.maxDepth and can tune it there, rather than having to know the key and add it by hand.
 *
 * The value below therefore has to stay equal to that code default and to the shipped cfg; changing the
 * default means changing all three. It also pins the value: once this patch has run, the instance carries
 * an explicit graphql.query.maxExpandedFields=2000 that nothing revises, so a later change of the shipped
 * default reaches a fresh install and leaves every instance this patch already ran on at 2000 until an
 * operator edits it.
 *
 * Idempotent and non-intrusive: if graphql.query.maxExpandedFields is already set to an explicit value (an
 * administrator's choice, including a deliberate 0 to lift the bound), it is left untouched; the property
 * is only added when it is absent.
 */
def setQueryExpandedFieldLimit() {
    def key = "graphql.query.maxExpandedFields"
    def value = "2000"

    def configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
    def config = configAdmin.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
    if (config == null) {
        log.warn("No default GraphQL provider configuration found; cannot write the expanded field limit.")
        return
    }

    def props = config.getProperties()
    def current = props?.get(key)
    if (current != null && !current.toString().trim().isEmpty()) {
        log.info("GraphQL expanded field limit already configured (${key}=${current}); leaving as-is.")
        return
    }

    if (props == null) {
        props = new java.util.Hashtable()
    }
    props.put(key, value)
    config.update(props)
    log.info("GraphQL expanded field limit written to the default configuration (${key}=${value}): an " +
            "operation may execute up to ${value} fields once its fragments are expanded, and the property can " +
            "now be tuned there. Set 0 to lift the bound.")
}

setQueryExpandedFieldLimit()
