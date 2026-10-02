---
graphql-core: minor
---

Added an optional `params` argument to the node `url` field, for example `url(params: ["w:640"])`, so a module can return a sized file URL.

Without the argument, or without a module that reads the parameters, the field returns the same URL as before.
