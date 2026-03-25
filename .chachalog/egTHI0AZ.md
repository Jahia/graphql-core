---
# Allowed version bumps: patch, minor, major
graphql-core: patch
---

Revert role permission checks and logging added to grantRole/revertRole mutation APIs in #582, #575 (#591)
 - Checks are now done at the JCR level as part of the story https://github.com/Jahia/jahia-private/issues/4730
