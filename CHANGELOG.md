# graphql-core Changelog

## 3.9.0

### New Features

* Mutations that operate on many nodes at once are now bounded by a dedicated configuration property, `graphql.mutation.batch.limit` (default `5000`, `0` disables), read only from the default configuration file so a non-default configuration cannot loosen it.

  **Are you affected?** Only if a single request operates on more than 5000 nodes. Such a request now fails rather than being applied to a subset, and nothing is persisted. Split the work into smaller requests, using `limit`/`offset` for `mutateNodesByQuery`, or raise the property in `org.jahia.modules.graphql.provider-default.cfg`.

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
