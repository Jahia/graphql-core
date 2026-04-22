import org.jahia.osgi.BundleUtils
import org.osgi.framework.Bundle

Bundle bundle = BundleUtils.getBundle("BUNDLE_KEY", "BUNDLE_VERSION")

if (bundle == null) {
    throw new IllegalStateException("Bundle 'BUNDLE_KEY'/'BUNDLE_VERSION' not found. Is it installed?")
}


log.info("== CHECK  \"BUNDLE_KEY\" \"BUNDLE_VERSION\"  == module state {}", bundle.getState());
// EXPECTED_STATE is substituted with a Bundle constant name, e.g. ACTIVE or RESOLVED.
// If the current state does not match, the script throws so the runner yields '.failed'.
// If it matches, the runner yields '.installed'.

if ("EXPECTED_STATE".equals("RESOLVED") and bundle.getState() == Bundle.ACTIVE) {
    throw new IllegalStateException(
        "Bundle 'BUNDLE_KEY' state mismatch: expected EXPECTED_STATE (${Bundle.EXPECTED_STATE}), actual ${bundle.getState()}"
    )
} else if ("EXPECTED_STATE".equals("ACTIVE") and bundle.getState() != Bundle.ACTIVE) {
    throw new IllegalStateException(
            "Bundle 'BUNDLE_KEY' state mismatch: expected EXPECTED_STATE (${Bundle.EXPECTED_STATE}), actual ${bundle.getState()}"
    )
}

