import org.osgi.framework.Bundle

// Check whether any installed bundle has the given symbolic name (any version).
// Throws if none found so that cy.executeGroovy() yields '.failed' in that case.
Bundle[] bundles = bundleContext.getBundles()
boolean found = bundles.any { it.symbolicName == "MODULE_KEY" }

if (!found) {
    throw new IllegalStateException("Bundle 'MODULE_KEY' is not installed (any version)")
}
