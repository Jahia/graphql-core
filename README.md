# GraphQL framework 

This module provides a base framework to deploy GraphQL APIs inside DX. It provides a common endpoint servlet at /modules/graphql , with base Query, Mutation and Subscription.

These types can be extended with new fields. The servlet looks for services implementing the interface `DXGraphQLExtensionsProvider` and will register them into the endpoint. A main API for DX is provided by [DXM provider](./graphql-dxm-provider). Custom bundles can extend the API by adding new fields to existing types, that could alos return new types. 
