---
graphql-core: minor
---

Mutations that operate on many nodes at once are now bounded by a dedicated configuration property, `graphql.mutation.batch.limit` (default `5000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

**Are you affected?** Only if a single request operates on more than 5000 nodes. Such a request now fails rather than being applied to a subset, and nothing is persisted. Split the work into smaller requests, using `limit`/`offset` for `mutateNodesByQuery`, or raise the property in `org.jahia.modules.graphql.provider-default.cfg`.
