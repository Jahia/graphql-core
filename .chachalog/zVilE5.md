---
graphql-core: minor
---

**Behaviour change** — reading the decrypted value of a property (`decryptedValue`, `decryptedValues`) now requires the new "Read the decrypted value of an encrypted property" permission (`viewEncryptedValue`) on the node that holds the property.

**Are you affected?** Only if an account reading those fields is neither a server administrator nor a system administrator, in which case the single-valued field answers `null` and the multi-valued one answers an empty list. Grant that account a role carrying the permission to restore access. Every other field of a property is unaffected, and no configuration changes this.
