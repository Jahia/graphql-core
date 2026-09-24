---
graphql-core: patch
---

The `contains` and `like` descriptions in `nodeConstraint` now say which store each operator reads, and how each one matches a substring and accents. `contains` searches the index: whole words match without case or accents, and a substring needs `*term*` written lowercase and without accents. `like` reads the stored value: case and accents must match exactly, and a substring needs `%term%`.

**Are you affected?** No. Only the schema descriptions changed, and query behaviour is the same.
