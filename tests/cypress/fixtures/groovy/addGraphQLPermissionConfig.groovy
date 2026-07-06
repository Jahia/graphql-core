import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
// Create a factory configuration instance for DXGraphQLConfig.
// ManagedServiceFactory.updated() is only triggered by createFactoryConfiguration(),
// NOT by getConfiguration() or ConfigService.getConfig(pid, identifier) which create
// singleton configs that do not match the factory's service.pid.
//
// PERMISSION_KEY format : "permission.TypeName.fieldName" (e.g. "permission.Query.schemaUpdatePing")
// PERMISSION_VALUE       : permission name to require     (e.g. "schemaUpdateNonExistent")
def config = ca.createFactoryConfiguration("org.jahia.modules.graphql.provider", null)
def props = new Hashtable()
props.put("PERMISSION_KEY", "PERMISSION_VALUE")
props.put("test.schemaupdate", "true")
config.update(props)
