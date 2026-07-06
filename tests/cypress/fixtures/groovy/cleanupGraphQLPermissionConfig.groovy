import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
// Remove all factory config instances created by addGraphQLPermissionConfig.groovy.
// Identified by the "test.schemaupdate=true" marker property.
def configs = ca.listConfigurations("(&(service.factoryPid=org.jahia.modules.graphql.provider)(test.schemaupdate=true))")
configs?.each { it.delete() }
