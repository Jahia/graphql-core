---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added query-cost limits to the GraphQL endpoint to protect against expensive or abusive queries. Queries exceeding the configured complexity or depth are now rejected before execution. The limits are enabled by default (`graphql.query.maxComplexity = 2000`, `graphql.query.maxDepth = 30`) and can be tuned, or disabled by setting either property to `0`, in the GraphQL provider configuration.
