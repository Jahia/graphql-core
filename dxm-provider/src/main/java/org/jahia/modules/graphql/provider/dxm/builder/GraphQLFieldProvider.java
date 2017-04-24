package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;

import java.util.List;

public interface GraphQLFieldProvider {

    String getTypeName();

    List<GraphQLFieldDefinition> getFields();

}
