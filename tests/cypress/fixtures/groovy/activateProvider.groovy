import org.jahia.osgi.BundleUtils
import org.osgi.service.cm.ConfigurationAdmin

def ca = BundleUtils.getOsgiService(ConfigurationAdmin.class, null)

// Deactivate all other providers first to enforce mutual exclusion.
// PROVIDERS_TO_DEACTIVATE is a pipe-separated list of PIDs; may be empty.
def othersToDeactivate = "PROVIDERS_TO_DEACTIVATE"
if (othersToDeactivate.trim()) {
    othersToDeactivate.split("\\|").each { pid ->
        def existing = ca.getConfiguration(pid.trim(), null)
        if (existing.properties != null) {
            existing.delete()
        }
    }
}

// Activate the target provider by creating its required configuration.
// Idempotent: does nothing if the config already exists.
def config = ca.getConfiguration("PROVIDER_PID", null)
if (config.properties == null) {
    config.update(new Hashtable())
}
