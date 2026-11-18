/**
 * Removes the org.jahia.bundles.api.authorization-subscription-scope-test.yml file
 * from Karaf's etc directory.
 *
 * Safe to call even when the file does not exist (idempotent).
 */

def karafEtc = System.getProperty("karaf.etc")

def file = new File(karafEtc, "org.jahia.bundles.api.authorization-subscription-scope-test.yml")
if (file.exists()) {
    file.delete()
}

return ".installed"
