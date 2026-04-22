import org.jahia.osgi.BundleUtils
import org.osgi.framework.Bundle

Bundle bundle = BundleUtils.getBundle("BUNDLE_KEY", "BUNDLE_VERSION")

if (bundle == null) {
    throw new IllegalStateException("Bundle 'BUNDLE_KEY' version 'BUNDLE_VERSION' not found. Is it installed?")
}

log.info("== START  \"BUNDLE_KEY\" \"BUNDLE_VERSION\"  == module state {}", bundle.getState());

if (bundle.getState() == Bundle.ACTIVE) {
    // Already started – nothing to do
    return
}

bundle.start()
