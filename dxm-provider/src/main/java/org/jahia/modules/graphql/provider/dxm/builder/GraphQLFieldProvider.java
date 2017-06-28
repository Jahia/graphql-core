package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;

import java.util.Collection;

public interface GraphQLFieldProvider {

    String getTypeName();

    Collection<GraphQLFieldDefinition> getFields();

}
