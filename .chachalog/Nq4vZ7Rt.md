---
graphql-core: minor
---

The number of nodes a single GraphQL request may read is now bounded by a new configuration property, `graphql.fields.node.requestLimit` (default `20000`, `0` disables).
