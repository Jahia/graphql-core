---
graphql-core: minor
---

Limited how many GraphQL subscription connections stay open at once and how many subscriptions each one runs.

**Are you affected?** Only if a client opens subscriptions without the `graphql-ws` subprotocol, with an account whose permissions allow no subscription, or beyond the limits that the `graphql.websocket.*` properties of `org.jahia.modules.graphql.provider-default.cfg` set. The Jahia interface opens one connection per browser tab, so an account shared by several people can reach the default limit of 20 connections.

The limits and their defaults are `graphql.websocket.maxConnections` (1000 connections in total), `graphql.websocket.maxConnectionsPerUser` (20 connections per user), `graphql.websocket.maxSubscriptionsPerConnection` (50 subscriptions per connection) and `graphql.websocket.initTimeout` (10 seconds to initialise a connection). A value of `0` disables a limit.
