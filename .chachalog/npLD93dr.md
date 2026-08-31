---
graphql-core: minor
---

The number of operations a single GraphQL request may submit is now bounded by a new configuration property, `graphql.request.operationLimit` (default `20`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

**Are you affected?** Only if a client sends more than 20 operations in one request, by posting them as a JSON array. Such a request now fails as a whole rather than being answered in part; send the operations in separate requests, or in smaller groups. One operation per request — what Jahia's own interfaces send, and what the common GraphQL clients do by default — is unaffected. You can also raise the property in `org.jahia.modules.graphql.provider-default.cfg`.
