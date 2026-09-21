---
graphql-core: patch
---

The `contains` and `like` descriptions in `nodeConstraint` now say which store each operator reads. `contains` searches the index, so case and accents are ignored and words match by their stem, and its wildcard is `*`. `like` reads the stored value, so case and accents must match exactly, and its wildcards are `%` and `_`.

**Are you affected?** No. Only the schema descriptions changed, and query behaviour is the same.
