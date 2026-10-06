---
graphql-core: minor
---

**Removed** — the Tag Manager GraphQL API (`admin { jahia { tagManager(siteKey) } }`, queries and mutations) is removed from graphql-core. jContent now provides the Tag Manager API itself, under `jcontent { tagManager(siteKey) }`, and no longer needs it here. That API also only worked for server administrators: `admin.jahia` requires server-level permissions, so users holding only "Access to tag manager" on a site were refused.

**Are you affected?** Only if a script or integration calls `admin { jahia { tagManager … } }`. Switch to `jcontent { tagManager(siteKey: …) { … } }` from jContent: the fields are the same, and access only requires the "Access to tag manager" permission on the site.
