---
graphql-core: patch
---

SDL finder arguments are now consistently treated as literal values when they are placed into the generated JCR-SQL2 query, across the String, weak-reference and date finders. A value containing an apostrophe is matched as exactly the text it contains, an empty value is accepted where it previously failed, and an argument passed explicitly as null is treated as absent rather than as a value to match on.
