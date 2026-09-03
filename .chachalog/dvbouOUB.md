---
graphql-core: minor
---

Limited how many fields a GraphQL query may execute once its fragments are expanded, through the new `graphql.query.maxExpandedFields` property (default `2000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

**Are you affected?** Only if a query spreads the same fragment repeatedly inside nested fields so that it executes more than 2000 fields; such a query is now refused before execution with `Maximum field count exceeded`. Jahia's own interfaces stay far below the limit. Raise the property in `org.jahia.modules.graphql.provider-default.cfg`, or set it to `0` to lift the bound.
