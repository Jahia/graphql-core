import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

// Sets the bound on how many operations one request may submit, on the DEFAULT provider configuration.
//
// The provider only honours graphql.request.operationLimit when it comes from the default configuration, so that a
// third-party module config cannot loosen it. It recognises the default configuration by its pid, so editing the
// existing "default" factory instance via getFactoryConfiguration(...,"default",...) is all that is needed here.
//
// Token OPERATION_LIMIT is substituted by cy.executeGroovy. Use 0 to disable the bound.
def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
def config = ca.getFactoryConfiguration("org.jahia.modules.graphql.provider", "default", null)
def props = config.getProperties()
if (props == null) {
    props = new java.util.Hashtable()
}
props.put("graphql.request.operationLimit", "OPERATION_LIMIT")
config.update(props)
