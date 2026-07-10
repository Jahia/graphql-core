---
graphql-core: patch
---

Propagate the authorization scopes resolved at connection time to the GraphQL WebSocket subscription execution thread, matching how the HTTP query/mutation executor already propagates them. Previously subscription data fetchers ran without the connection's scopes initialized, so field-level permission checks were not applied consistently on the WebSocket transport; they are now enforced the same way as on HTTP requests.
