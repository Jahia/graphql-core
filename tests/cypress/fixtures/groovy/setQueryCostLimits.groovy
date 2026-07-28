import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

// Sets the GraphQL query-cost guards on the DEFAULT provider configuration.
//
// The provider only honours graphql.query.maxComplexity / maxDepth / maxListNesting when they come from the default
// configuration, so that a third-party module config cannot loosen them. It recognises the default configuration by
// its pid, so editing the existing "default" factory instance via getFactoryConfiguration(...,"default",...) is all
// that is needed here.
//
// Tokens MAX_COMPLEXITY / MAX_DEPTH / MAX_LIST_NESTING are substituted by cy.executeGroovy. Use 0 to disable a guard.
def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
def config = ca.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
def props = config.getProperties()
if (props == null) {
    props = new java.util.Hashtable()
}
props.put("graphql.query.maxComplexity", "MAX_COMPLEXITY")
props.put("graphql.query.maxDepth", "MAX_DEPTH")
props.put("graphql.query.maxListNesting", "MAX_LIST_NESTING")
config.update(props)
