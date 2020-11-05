package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.bin.Jahia;

/**
 * GraphQL root object for Admin related mutations.
 */
@GraphQLName("adminMutation")
@GraphQLDescription("Admin mutations")
public class GqlAdminMutation {
    @GraphQLField
    @GraphQLName("version")
    @GraphQLNonNull
    @GraphQLDescription("Stub mutation to get version of the running Jahia instance")
    public String getProductVersion() {
        return Jahia.getFullProductVersion();
    }
}
