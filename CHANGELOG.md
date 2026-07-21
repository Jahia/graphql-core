# graphql-core Changelog

## 3.8.0

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
