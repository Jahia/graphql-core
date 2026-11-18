---
graphql-core: minor
---

Restricted GraphQL subscription connections to the address Jahia is served on.

A subscription connection that declares another origin is refused. Jahia's own interfaces are unaffected, provided that a reverse proxy in front of Jahia keeps the public host name and, when it ends TLS, sends the `X-Forwarded-Proto` header. No cross-origin setting applies to subscription connections: an origin trusted in `org.jahia.bundles.api.security.cfg`, through `cors.allowed.origins` or the `auto_apply` origin of a scope, keeps its HTTP GraphQL access but is refused on subscriptions, and so is an origin listed in `http.cors.allow-origin`. If a page on another website opens subscriptions, serve it from the same address as Jahia, for instance through your reverse proxy.
