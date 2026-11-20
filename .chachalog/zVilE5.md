---
graphql-core: minor
---

Changed the GraphQL API so that reading the decrypted value of a property requires the new "Read the decrypted value of an encrypted property" permission on the content that holds it.

**Are you affected?** Yes, if an account other than a server administrator or a system administrator reads `decryptedValue` or `decryptedValues`, or edits content that has a password field in the content editor. Without the permission, `decryptedValue` returns `null` and `decryptedValues` returns an empty list, and saving such content in the content editor removes the stored password.

**What to do:** before you upgrade, give each of these accounts a role that carries the `viewEncryptedValue` permission, for example the site administrators and editors who maintain content with a password field. Server administrators and system administrators receive the permission automatically.
