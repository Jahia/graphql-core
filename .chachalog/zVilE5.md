---
graphql-core: minor
---

Changed the GraphQL API so that reading the decrypted value of a property requires the new "Read the decrypted value of an encrypted property" permission, granted on the repository root.

**Are you affected?** Yes, if an account other than a server administrator or a system administrator reads `decryptedValue` or `decryptedValues`, or edits content that has a password field in the content editor. Without the permission, `decryptedValue` returns `null` and `decryptedValues` returns an empty list. Such an account can still replace a stored password, but cannot read it, and saving such content in the content editor without typing the password again removes the stored password.

**What to do:** server administrators and system administrators receive the permission automatically. To let another account read decrypted values, grant it a role that carries the `viewEncryptedValue` permission on the repository root (`/`). A grant on a site or on a content node has no effect: one key decrypts every value on the server, so the permission always applies to the whole server.
