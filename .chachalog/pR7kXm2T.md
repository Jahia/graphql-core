---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

Added a Tag Manager GraphQL API to list, rename, and delete tags across a site or on individual nodes.
 - Available under `admin.jahia.tagManager(siteKey)` with queries `tags` and `taggedContent`, and mutations `renameTag`, `deleteTag`, `renameTagOnNode`, and `deleteTagOnNode`.
 - All mutations propagate changes to both the edit and live workspaces automatically.
 - Requires the `tagManager` permission on the target site.
