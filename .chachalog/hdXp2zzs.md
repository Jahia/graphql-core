---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added the `versionAt` and `versionOperation` arguments to the `jcr` query root, to read the EDIT workspace at a version.

A query that reads a version is read-only and selects the `jcr` field alone. It needs Jahia 8.2.4 or later and content versioning.
