---
graphql-core: patch
---

Limited how many fields a GraphQL query may execute once its fragments are expanded, through the new `graphql.query.maxExpandedFields` property (default `2000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.
