package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

import javax.jcr.RepositoryException;

/**
 * A mutation extension that adds a possibility to alter JCR nodes.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public class NodeMutationExtensions {

    /**
     * Root for all JCR mutations
     */
    @GraphQLField
    @GraphQLName("jcr")
    @GraphQLDescription("JCR Mutation")
    public static GqlJcrMutation getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) throws RepositoryException {
        return new GqlJcrMutation(workspace);
    }

}
