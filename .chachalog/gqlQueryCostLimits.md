---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added query-cost limits to the GraphQL endpoint to protect against expensive or abusive queries. Queries exceeding the configured complexity or depth are now rejected before execution. The limits are enabled by default (`graphql.query.maxComplexity = 2000`, `graphql.query.maxDepth = 30`) and can be tuned, or disabled by setting either property to `0`, in the GraphQL provider configuration. These limits, like the existing node limit, are only accepted from the default configuration file so a non-default configuration cannot loosen them; a configuration that tries is now logged instead of being silently ignored. Removing a limit property (or deleting the configuration) reverts it to its default rather than keeping the last configured value.
