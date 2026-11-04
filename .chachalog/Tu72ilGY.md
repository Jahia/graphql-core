---
graphql-core: minor
---

**Behaviour change** — GraphQL subscription connections (`/modules/graphqlws`) are accepted only from the address Jahia is served on and from the origins listed in `http.cors.allow-origin`.

**Are you affected?** Only if subscriptions are opened from another website, or by a script or server-side integration. Jahia's own interfaces are unaffected. For a page on another website, add its address to `http.cors.allow-origin` in an `org.jahia.modules.graphql.provider-*.cfg` file; as for its GraphQL requests, that page also needs access in the API security configuration. A script or integration must send an `Origin` header with the address Jahia is served on, or with an address from that list: a connection without one is refused.
