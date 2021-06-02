package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * GraphQL root object for Admin related mutations.
 */
@GraphQLName("AdminMutation")
@GraphQLDescription("Admin mutations")
public class GqlAdminMutation {
    /**
     * Get Jahia admin query
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get Jahia admin mutation")
    @GraphQLRequiresPermission(value = "admin")
    public GqlJahiaAdminMutation getJahia() {
        return new GqlJahiaAdminMutation();
    }

}
