---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Fixed the default GraphQL configuration file so Jahia no longer writes a corrupted character into it on a new install.

**Are you affected?** This change does not repair `org.jahia.modules.graphql.provider-default.cfg` on an existing install.
