---
page:
  '$path': '/sites/academy/home/documentation/jahia/8_2/developer/working-with-our-apis/graphql-api/graphql-query-cost-limits'
  'jcr:title': Query cost limits (DoS protection)
  'j:templateName': documentation
content:
  '$subpath': document-area/content
---

Starting with graphql-core 3.9.0, the GraphQL endpoint can reject requests whose execution cost would be excessive, protecting a Jahia instance against accidental or malicious query amplification. All of the settings below are read exclusively from the default provider configuration file (`org.jahia.modules.graphql.provider-default.cfg`), so a third-party module configuration cannot loosen them. Setting a value to `0` disables that specific guard.

**`graphql.query.maxComplexity`** (default: `2000`)
Maximum complexity of a query document, where every field, alias and meta field counts as 1 plus the complexity of its sub-selection. Documents over the limit are rejected before execution starts.

**`graphql.query.maxDepth`** (default: `30`)
Maximum nesting depth of a query document, also checked before execution.

**`graphql.fields.node.requestLimit`** (default: `20000`)
Maximum number of JCR nodes a single request may read across all of its fields together. Unlike the two guards above, this one is enforced during execution and counts the nodes actually read, which is what bounds the fan-out of nested list fields (for example `descendants` inside `descendants`) that no static check can predict.

Be careful with low values: a connection's `totalCount` is computed by reading every node the connection matches, and those reads count against this limit. Content-heavy websites and editing UIs such as jContent select `totalCount` routinely, so a limit lower than the largest listing on the site will cause those requests to fail. When enabling this limit, choose a value comfortably above the number of nodes the heaviest page or view reads, and validate on a staging instance with production-scale content.

**`graphql.mutation.batch.limit`** (default: `5000`)
Maximum number of nodes one request may ask a mutation to operate on (`mutateNodes`, `mutateNodesByQuery`, `addNodesBatch`). A request over the limit fails as a whole; it is never applied to a subset of the requested nodes.

**`graphql.request.operationLimit`** (default: `20`)
Maximum number of operations one request may submit. The endpoint accepts a JSON array as a request body, each element of which is an independent operation executed as part of the same request; this is the number of those elements. It is the one limit here that is not a measure of a single operation, and so is the factor the others are multiplied by: a request may ask for as much work as this many operations at the full complexity, depth, node and mutation-batch ceilings.

A request that submits more fails as a whole, with `400`, and the response reports how many operations were submitted and what the maximum is; it is never trimmed down to the bound. Clients that batch typically default to ten operations per request, and Jahia's own interfaces send one, so the default leaves room for both.

**`graphql.fields.node.limit`** (default: `5000`)
The pre-existing cap on how many nodes a single connection (paginated field) may collect. As of 3.9.0 it can also be set to `0` to disable the cap.
