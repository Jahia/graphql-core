package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * GraphQL root object for Admin related mutations.
 */
@GraphQLName("JahiaAdminMutation")
@GraphQLDescription("Admin mutations")
public class GqlJahiaAdminMutation {

    /**
     * We must have at least one field for the schema to be valid
     *
     * @return true
     */
    @GraphQLField
    public boolean noop() {
        return true;
    }
}
