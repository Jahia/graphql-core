import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

// Removes the query-cost guard properties from the DEFAULT provider configuration, simulating an operator dropping
// them (or an upgrade where they were never set). The effective limits must then revert to their code default rather
// than sticking at the last configured value: 0 (disabled) for the complexity and depth guards, and the built-in
// value for the list-nesting guard, which stays on so an upgraded deployment keeps that protection.
def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
def config = ca.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
def props = config.getProperties()
if (props != null) {
    props.remove("graphql.query.maxComplexity")
    props.remove("graphql.query.maxDepth")
    props.remove("graphql.query.maxListNesting")
    config.update(props)
}
