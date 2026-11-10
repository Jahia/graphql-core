---
graphql-core: minor
---

Restricted GraphQL subscription connections to Jahia's own address and the origins in `http.cors.allow-origin`.

A subscription connection from any other origin, or one that declares no origin, is refused. Jahia's own interfaces are unaffected. If a page on another website opens subscriptions, add its address to `http.cors.allow-origin` in an `org.jahia.modules.graphql.provider-*.cfg` file; as for its GraphQL requests, that page also needs access in the API security configuration. If a script or a server-side integration opens subscriptions, make it send an `Origin` header with the Jahia address or with an address from that list.
