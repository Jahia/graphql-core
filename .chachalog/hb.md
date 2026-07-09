---
graphql-core: patch
---

Enable the GraphQL introspection permission check by default; schema introspection now requires the developerToolsAccess permission (override introspectionCheckEnabled=false to restore the previous behaviour).
