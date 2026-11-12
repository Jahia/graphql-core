---
graphql-core: minor
---

Two modules that add a field with the same name to one GraphQL type no longer stop the GraphQL API. Before this fix, the GraphQL API failed to start, and the Jahia interfaces that use it stopped working.

The module whose bundle symbolic name comes first in alphabetical order keeps the field. Jahia registers none of the GraphQL extensions that the other module declares in Java. Jahia logs an error that names the field, the type and both modules. The `graphql-extension.sdl` file of the other module still registers.

**Are you affected?** Only if two of your modules add the same field to one GraphQL type. To get the GraphQL extensions of both modules, remove the field from one module or rename it.
