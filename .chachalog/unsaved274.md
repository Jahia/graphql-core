---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added the `unsavedValues` argument to the `jcr` query root, to read the EDIT workspace with the values of a form that the user has not saved.

The node of the form shows these values, and every other node shows its current content. The query writes nothing and takes no lock, and it follows the rules of `versionAt`. It needs Jahia 8.2.4 or later.
