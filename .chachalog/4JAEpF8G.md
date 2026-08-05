---
graphql-core: patch
---

Tag Manager mutations now only write the nodes the caller has rights on, and refuse a replacement tag name that the configured tag handler reduces to nothing
