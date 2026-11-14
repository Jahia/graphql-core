---
graphql-core: minor
---

Restricted GraphQL subscription connections to the address Jahia is served on.

A subscription connection that declares another origin is refused. Jahia's own interfaces are unaffected, provided that a reverse proxy in front of Jahia keeps the public host name and, when it ends TLS, sends the `X-Forwarded-Proto` header. If a page on another website opens subscriptions, serve it from the same address as Jahia, for instance through your reverse proxy. The `http.cors.allow-origin` setting does not apply to subscription connections.
