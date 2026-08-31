---
page:
  '$path': '/sites/academy/home/documentation/jahia/8_2/developer/working-with-our-apis/graphql-api/graphql-query-cost-limits'
  'jcr:title': Query cost limits (DoS protection)
  'j:templateName': documentation
content:
  '$subpath': document-area/content
---

Starting with graphql-core 3.9.1, the GraphQL endpoint can reject requests whose execution cost would be excessive, protecting a Jahia instance against accidental or malicious query amplification. All of the settings below are read exclusively from the default provider configuration file (`org.jahia.modules.graphql.provider-default.cfg`), so a third-party module configuration cannot loosen them. Setting a value to `0` disables that specific guard.

**`graphql.query.maxComplexity`** (default: `2000`)
Maximum complexity of a query document, where every field, alias and meta field counts as 1 plus the complexity of its sub-selection. Documents over the limit are rejected before execution starts.

**`graphql.query.maxDepth`** (default: `30`)
Maximum nesting depth of a query document, also checked before execution.

**`graphql.fields.node.requestLimit`** (default: `0`, disabled)
Maximum number of JCR nodes a single request may read across all of its fields together. Unlike the two guards above, this one is enforced during execution and counts the nodes actually read, which is what bounds the fan-out of nested list fields (for example `descendants` inside `descendants`) that no static check can predict.

Be careful with low values: a connection's `totalCount` is computed by reading every node the connection matches, and those reads count against this limit. Content-heavy websites and editing UIs such as jContent select `totalCount` routinely, so a limit lower than the largest listing on the site will cause those requests to fail. When enabling this limit, choose a value comfortably above the number of nodes the heaviest page or view reads, and validate on a staging instance with production-scale content.

**`graphql.mutation.batch.limit`** (default: `5000`)
Maximum number of nodes one request may ask a mutation to operate on (`mutateNodes`, `mutateNodesByQuery`, `addNodesBatch`). A request over the limit fails as a whole; it is never applied to a subset of the requested nodes.

**`graphql.fields.node.limit`** (default: `5000`)
The pre-existing cap on how many nodes a single connection (paginated field) may collect. As of 3.9.1 it can also be set to `0` to disable the cap.
