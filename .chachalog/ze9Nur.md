---
graphql-core: minor
---

Restricted the GraphQL user and group administration queries (`admin.userAdmin`, `admin.userGroup`) to callers holding the GraphQL administration permission, and limited the `property(name:)` field of a user or group to the profile and membership values those types publish.

An integration that reads either query under an account other than a server administrator must be granted the "Perform GraphQL queries under the admin node" permission (`graphqlAdminQuery`) on that account's role.
