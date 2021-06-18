package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.osgiconfig.GqlConfigurationMutation;

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
    @GraphQLDescription("Mutate an OSGi configuration")
    public GqlConfigurationMutation configuration(@GraphQLName("pid") @GraphQLDescription("Configuration pid ot factory pid") @GraphQLNonNull String pid,
                                                  @GraphQLName("identifier") @GraphQLDescription("If factory pid, configiration identifier (filename suffix)") String identifier) {
        return new GqlConfigurationMutation(pid, identifier);
    }
}
