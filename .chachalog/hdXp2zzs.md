---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added the `versionAt` and `versionOperation` arguments to the `jcr` query root, so that a GraphQL client reads the EDIT workspace at a version. `versionAt` takes a date in milliseconds since the epoch, and `versionOperation` takes the UUID of an operation. The versions come from the content-versioning module, and the arguments need Jahia 8.2.4 or later.

A query of a version is read-only, and it selects the `jcr` field alone at its root. It is also the only operation of its request, and it cannot render `renderedContent` in edit mode. A field annotated `@GraphQLAsync` reads the same version.

Fixed `properties(language:)`, which failed on a node of a version because such a property has no raw JCR property.
