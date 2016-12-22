package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;

import java.util.List;


public interface DXGraphQLExtender {

    List<GraphQLFieldDefinition> getFields();
}
