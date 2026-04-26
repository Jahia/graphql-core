/**
 * Writes the org.jahia.bundles.api.authorization-schema-update-test.yml file
 * to Karaf's etc directory, declaring a scope that grants API access to the
 * schemaUpdatePermTest GraphQL field for hosted-origin requests.
 *
 * This is the API-level gate: all users (including the unprivileged test user) can
 * reach the field endpoint. The @GraphQLRequiresPermission annotation on the field
 * then provides the second layer of access control.
 *
 * Safe to call multiple times (idempotent file write).
 */

def karafEtc = System.getProperty("karaf.etc")

def yamlContent = """\
schemaUpdateTest:
  auto_apply:
  - origin: "hosted"
  grants:
  - node: "none"
    api: "graphql.Query.schemaUpdatePermTest"
"""

def file = new File(karafEtc, "org.jahia.bundles.api.authorization-schema-update-test.yml")
file.text = yamlContent

return ".installed"
