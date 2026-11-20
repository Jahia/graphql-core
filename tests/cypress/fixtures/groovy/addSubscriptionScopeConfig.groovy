/**
 * Writes the org.jahia.bundles.api.authorization-subscription-scope-test.yml file
 * to Karaf's etc directory, declaring a scope that applies to every request and
 * grants one subscription field, workflowEvent, and no other.
 *
 * Safe to call multiple times (idempotent file write).
 */

def karafEtc = System.getProperty("karaf.etc")

def yamlContent = """\
subscriptionScopeTest:
  auto_apply:
  - always: true
  grants:
  - node: "none"
    api: "graphql.Subscription.workflowEvent"
"""

def file = new File(karafEtc, "org.jahia.bundles.api.authorization-subscription-scope-test.yml")
file.text = yamlContent

return ".installed"
