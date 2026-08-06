---
graphql-core: patch
---

Tag Manager mutations now only write the nodes the caller has rights on, and refuse a replacement tag name that would be stored as an empty tag or as no tag at all. Note one consequence of the per-node scoping: a site-wide rename or delete no longer touches a node that exists only in the live workspace — published, then removed from the edit workspace — so tags left on such nodes are no longer swept by these mutations
