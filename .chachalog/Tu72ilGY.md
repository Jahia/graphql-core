---
graphql-core: minor
---

Restricted GraphQL subscription connections to the address Jahia is served on.

A subscription connection from any other origin, or one that declares no origin, is refused. Jahia's own interfaces are unaffected, provided that a reverse proxy which ends TLS in front of Jahia sends the `X-Forwarded-Proto` header. If a page on another website opens subscriptions, serve it from the same address as Jahia, for instance through your reverse proxy. If a script or a server-side integration opens subscriptions, make it send an `Origin` header with the Jahia address.
