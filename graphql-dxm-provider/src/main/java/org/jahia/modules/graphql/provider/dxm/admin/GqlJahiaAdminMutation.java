package org.jahia.modules.graphql.provider.dxm.admin;


import graphql.annotations.annotationTypes.*;
import graphql.execution.Async;
import graphql.schema.AsyncDataFetcher;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
    public GqlConfigurationMutation configuration(@GraphQLName("pid") @GraphQLNonNull String pid,
                                                  @GraphQLName("identifier") String identifier) {
        return new GqlConfigurationMutation(pid, identifier);
    }
}
