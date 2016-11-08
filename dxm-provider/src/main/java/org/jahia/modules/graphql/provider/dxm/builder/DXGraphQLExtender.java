package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLObjectType;


public interface DXGraphQLExtender {

    GraphQLObjectType.Builder build(GraphQLObjectType.Builder builder);
}
