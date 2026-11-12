---
graphql-core: minor
---

Two modules that add a field with the same name to one GraphQL type no longer stop the GraphQL API. Before this fix, the GraphQL API failed to start, and the Jahia interfaces that use it stopped working.

The module that was installed first keeps the field. Jahia registers none of the GraphQL extensions of the other module, and logs an error that names the field, the type and both modules.

**Are you affected?** Only if two of your modules add the same field to one GraphQL type. To get the GraphQL extensions of both modules, remove the field from one module or rename it.
