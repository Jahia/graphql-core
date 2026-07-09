import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

// Sets the GraphQL query-cost guards on the DEFAULT provider configuration.
//
// The provider only honours graphql.query.maxComplexity / graphql.query.maxDepth when they originate from the
// default config file (it checks felix.fileinstall.filename ends with org.jahia.modules.graphql.provider-default.cfg,
// so a third-party module config cannot loosen them). We therefore edit the EXISTING "default" factory instance via
// getFactoryConfiguration(...,"default",...): its felix.fileinstall.filename is preserved across the update, so the
// new values pass that gate. createFactoryConfiguration() would NOT work here (no filename -> values ignored).
//
// Tokens MAX_COMPLEXITY / MAX_DEPTH are substituted by cy.executeGroovy. Use 0 to disable a guard.
def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
def config = ca.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
def props = config.getProperties()
if (props == null) {
    props = new java.util.Hashtable()
}
props.put("graphql.query.maxComplexity", "MAX_COMPLEXITY")
props.put("graphql.query.maxDepth", "MAX_DEPTH")
config.update(props)
