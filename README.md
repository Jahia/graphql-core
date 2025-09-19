![GitHub tag (latest by version)](https://img.shields.io/github/v/tag/Jahia/graphql-core?sort=semver)
![License](https://img.shields.io/github/license/jahia/graphql-core)

<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

GraphQL Core
======================


This module provides a base framework to deploy GraphQL APIs inside Jahia. It provides a common endpoint servlet at /modules/graphql, with base Query, Mutation and Subscription.

These types can be extended with new fields. The servlet looks for services implementing the interface `DXGraphQLExtensionsProvider` and will register them into the endpoint. A main API for DX is provided by [DXM provider](./graphql-dxm-provider). Custom bundles can extend the API by adding new fields to existing types, that could alos return new types. 

## Open-Source

This is an Open-Source module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source)
