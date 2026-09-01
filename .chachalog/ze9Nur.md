---
graphql-core: minor
---

**Behaviour change** — the GraphQL user and group administration queries (`admin.userAdmin`, `admin.userGroup`) require the GraphQL administration permission, and the `property(name:)` field of a user or group answers for profile and membership values.

**Are you affected?** Two independent checks. For the administration queries: only if the account making the call is not a server administrator, in which case those queries answer with a permission error. Grant that account a role carrying "Perform GraphQL queries under the admin node" (`graphqlAdminQuery`) to restore access.

For `property(name:)` on a `User`, `CurrentUser` or `Group`: only if the name it asks for is the account's stored password digest (`j:password`), which the field answers `null` for. Every other property name, custom ones included, is unaffected, and no configuration changes this. A caller that verifies a password should authenticate instead.
