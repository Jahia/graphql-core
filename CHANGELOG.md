# graphql-core Changelog

## 3.12.0

* Fixed the default GraphQL configuration file so Jahia no longer writes a corrupted character into it on a new install.

  **Are you affected?** This change does not repair `org.jahia.modules.graphql.provider-default.cfg` on an existing install. The corrupted character is in a comment, so it changes no GraphQL setting. To clean the file, replace it with a plain hyphen.

## 3.11.0

### New Features

* **Behaviour change** — the GraphQL `mutateWorkflows` mutation and its `abortWorkflow` field require the GraphQL administration mutation permission.

  **Are you affected?** Only if a script or integration calls `mutateWorkflows` with an account that is not a server administrator. That call now answers with a permission error. Grant the account a role carrying "Perform GraphQL mutations under the admin node" (`graphqlAdminMutation`) to restore access. Jahia's own interfaces do not use this mutation.

* Limited how many GraphQL subscription connections stay open at once and how many subscriptions each one runs.

  **Are you affected?** Only if a client opens subscriptions without the `graphql-ws` subprotocol, with an account whose permissions allow no subscription, or beyond the limits that the `graphql.websocket.*` properties of `org.jahia.modules.graphql.provider-default.cfg` set. The Jahia interface opens one connection per browser tab, so an account shared by several people can reach the default limit of 20 connections.

  The limits and their defaults are `graphql.websocket.maxConnections` (1000 connections in total), `graphql.websocket.maxConnectionsPerUser` (20 connections per user), `graphql.websocket.maxSubscriptionsPerConnection` (50 subscriptions per connection) and `graphql.websocket.initTimeout` (10 seconds to initialise a connection). A value of `0` disables a limit.

* Restricted GraphQL subscription connections to the address Jahia is served on.

  A subscription connection that declares another origin is refused. Jahia's own interfaces are unaffected, provided that a reverse proxy in front of Jahia keeps the public host name and, when it ends TLS, sends the `X-Forwarded-Proto` header. No cross-origin setting applies to subscription connections: an origin trusted in `org.jahia.bundles.api.security.cfg`, through `cors.allowed.origins` or the `auto_apply` origin of a scope, keeps its HTTP GraphQL access but is refused on subscriptions, and so is an origin listed in `http.cors.allow-origin`. If a page on another website opens subscriptions, serve it from the same address as Jahia, for instance through your reverse proxy.

* Limited how many fields a GraphQL query may execute once its fragments are expanded, through the new `graphql.query.maxExpandedFields` property (default `2000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

  **Are you affected?** Only if a query spreads the same fragment repeatedly inside nested fields so that it executes more than 2000 fields; such a query is now refused before execution with `maximum query expanded field count exceeded`. Jahia's own interfaces stay far below the limit. Raise the property in `org.jahia.modules.graphql.provider-default.cfg`, or set it to `0` to lift the bound.

* Two modules that add a field with the same name to one GraphQL type no longer stop the GraphQL API. Before this fix, the GraphQL API failed to start, and the Jahia interfaces that use it stopped working.

  The module whose bundle symbolic name comes first in alphabetical order keeps the field. Jahia registers none of the GraphQL extensions that the other module declares in Java. Jahia logs an error that names the field, the type and both modules. The `graphql-extension.sdl` file of the other module still registers.

  **Are you affected?** Only if two of your modules add the same field to one GraphQL type. To get the GraphQL extensions of both modules, remove the field from one module or rename it.

### Bug Fixes

* Added maven.jahia.com to the Maven repositories graphql-core uses to download its dependencies.

## 3.10.0

### New Features

* The number of operations a single GraphQL request may submit is now bounded by a new configuration property, `graphql.request.operationLimit` (default `20`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

  **Are you affected?** Only if a client sends more than 20 operations in one request, by posting them as a JSON array. Such a request now fails as a whole rather than being answered in part; send the operations in separate requests, or in smaller groups. One operation per request — what Jahia's own interfaces send, and what the common GraphQL clients do by default — is unaffected. You can also raise the property in `org.jahia.modules.graphql.provider-default.cfg`.

* **Behaviour change** — the GraphQL user and group administration queries (`admin.userAdmin`, `admin.userGroup`) require the GraphQL administration permission, and the `property(name:)` field of a user or group answers for profile and membership values.

  **Are you affected?** Two independent checks. For the administration queries: only if the account making the call is not a server administrator, in which case those queries answer with a permission error. Grant that account a role carrying "Perform GraphQL queries under the admin node" (`graphqlAdminQuery`) to restore access.

  For `property(name:)` on a `User`, `CurrentUser` or `Group`: only if the name it asks for is the account's stored password digest (`j:password`), which the field answers `null` for. Every other property name, custom ones included, is unaffected, and no configuration changes this. A caller that verifies a password should authenticate instead.

## 3.9.1

### New Features

* Mutations that operate on many nodes at once are now bounded by a dedicated configuration property, `graphql.mutation.batch.limit` (default `5000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

  **Are you affected?** Only if a single request asks to operate on more than 5000 nodes. Such a request now fails rather than being applied to a subset, and nothing is persisted. Work through a larger result set in pages instead. A `limit` no wider than the bound selects a page of `mutateNodesByQuery`. It is applied as asked when the rest of the request leaves room for it. You can also raise the property in `org.jahia.modules.graphql.provider-default.cfg`.

* The number of nodes a single GraphQL request may read is now bounded by a new configuration property, `graphql.fields.node.requestLimit` (default `20000`, `0` disables).

* **Deprecation:** the time-zone-less date surface of JCR properties is deprecated. On `JCRProperty`, `notZonedDateValue` and `notZonedDateValues` are marked `@deprecated`; on the property mutations (`setValue`, `addValue`, etc.), the `NOT_ZONED_DATE` value of the `option` argument is deprecated. All of them keep working exactly as before; nothing is removed.

  Prefer the zoned variants: read a date with `value` / `values`, which return the JCR ISO-8601 form carrying the time zone offset, and write one by passing `type: DATE` with an offset-bearing ISO-8601 value and no `option`.

  The deprecated surface formats and parses in the *server's* default time zone and drops the offset, so the same string denotes a different instant depending on where it is read or written. This ambiguity is the reason for deprecation.

* **Behaviour change** — the `createVersion` field on JCR node mutations is deprecated and no longer creates a version: it is now an inert no-op that always returns `false`. Files remain versionable and are still versioned when published; what no longer happens is the extra version created at upload time (the one labelled `uploaded_at_<timestamp>`). In practice, uploading a file and then replacing it without publishing in between no longer leaves a JCR snapshot of the first revision. The field is kept in the schema so existing callers keep working unchanged. (#626)

### Bug Fixes

* Tag Manager mutations now only write the nodes the caller has rights on, and refuse a replacement tag name that would be stored as an empty tag or as no tag at all. Note one consequence of the per-node scoping: a site-wide rename or delete no longer touches a node that exists only in the live workspace — published, then removed from the edit workspace — so tags left on such nodes are no longer swept by these mutations

* SDL finder arguments are now consistently treated as literal values when they are placed into the generated JCR-SQL2 query, across the String, weak-reference and date finders. A value containing an apostrophe is matched as exactly the text it contains, an empty value is accepted where it previously failed, and an argument passed explicitly as null is treated as absent rather than as a value to match on.

## 3.8.1

### New Features

* Added query-cost limits to the GraphQL endpoint to protect against expensive or abusive queries. Queries exceeding the configured complexity or depth are now rejected before execution. The limits are enabled by default (`graphql.query.maxComplexity = 2000`, `graphql.query.maxDepth = 30`) and can be tuned, or disabled by setting either property to `0`, in the GraphQL provider configuration. These limits, like the existing node limit, are only accepted from the default configuration file so a non-default configuration cannot loosen them; a configuration that tries is now logged instead of being silently ignored. Removing a limit property (or deleting the configuration) reverts it to its default rather than keeping the last configured value.

* Added a Tag Manager GraphQL API to list, rename, and delete tags across a site or on individual nodes.

  * Available under `admin.jahia.tagManager(siteKey)` with queries `tags` and `taggedContent`, and mutations `renameTag`, `deleteTag`, `renameTagOnNode`, and `deleteTagOnNode`.
  * All mutations propagate changes to both the edit and live workspaces automatically.
  * Requires the `tagManager` permission on the target site.

### Bug Fixes

* Enable the GraphQL introspection permission check by default; schema introspection now requires the developerToolsAccess permission (override introspectionCheckEnabled=false to restore the previous behaviour).

* Propagate the authorization scopes resolved at connection time to the GraphQL WebSocket subscription execution thread, matching how the HTTP query/mutation executor already propagates them. Previously subscription data fetchers ran without the connection's scopes initialized, so field-level permission checks were not applied consistently on the WebSocket transport; they are now enforced the same way as on HTTP requests.

## 3.7.0

### New Features

* Remove dev-only SDL source-watcher coupling to external-provider-modules (#629)

* Bump bn.js from 4.12.0 to 4.12.3 (#613)

* Bump markdown-it from 14.1.0 to 14.1.1 (#612)

* Filter out jmix:hiddenNode types when getting children/descendants (#609)

* `renderedContent` now falls back to the default view (instead of cm) when rendering in page context with no view specified. (#617)

* Secured the qs library by enforcing version 6.15.2 or higher to address known vulnerabilities. (#614, #634)

### Bug Fixes

* Undeprecated `User.name` to address a GraphQL specification violation. (#633)

* Revert role permission checks and logging added to grantRole/revertRole mutation APIs in #582, #575 (#591)

  * Checks are now done at the JCR level as part of the story https://github.com/Jahia/jahia-private/issues/4730

* Fix: Clean up cached permissions before rebuilding the schema on graphql provider registration (#605)
