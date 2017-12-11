package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

/**
 * A query extension that adds a possibility to fetch nodes by their UUIDs, paths, or via an SQL2/Xpath query.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class NodeQueryExtensions {

    /**
     * Root for all JCR queries
     */
    @GraphQLField
    @GraphQLName("jcr")
    @GraphQLDescription("JCR Queries")
    public static GqlJcrQuery getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace) {
        return new GqlJcrQuery(workspace);
    }

}
