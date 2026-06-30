---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added optional query-cost limits to the GraphQL endpoint to protect against expensive or abusive queries. Queries exceeding the configured complexity or depth are now rejected before execution. The limits are disabled by default; enable and tune `graphql.query.maxComplexity` and `graphql.query.maxDepth` in the GraphQL provider configuration.
