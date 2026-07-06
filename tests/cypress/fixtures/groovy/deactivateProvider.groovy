import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)
// Delete the configuration for the given component PID.
// This removes the mandatory configuration and causes the component to deactivate.
def config = ca.getConfiguration("PROVIDER_PID", null)
if (config.properties != null) {
    config.delete()
}
