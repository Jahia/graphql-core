package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

/**
 * Info about a node to be reproduced at (moved or copied to) another parent node.
 */
@GraphQLName("CarriedJCRNode")
@GraphQLDescription("Info about a node to be reproduced at (moved or copied to) another parent node")
public class GqlJcrReproducibleNodeInput {

    private String pathOrId;
    private String destParentPathOrId;
    private String destName;

    /**
     * Create an instance of the input object.
     *
     * @param pathOrId Path or UUID of the node to be copied/moved
     * @param destParentPathOrId Path or UUID of the destination parent node to copy/move the node to
     * @param destName The name of the node at the new location or null if its current name should be preserved
     */
    public GqlJcrReproducibleNodeInput(
        @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be copied/moved") String pathOrId,
        @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to copy/move the node to") String destParentPathOrId,
        @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName
    ) {
        this.pathOrId = pathOrId;
        this.destParentPathOrId = destParentPathOrId;
        this.destName = destName;
    }

    @GraphQLField
    @GraphQLName("pathOrId")
    @GraphQLNonNull
    @GraphQLDescription("Path or UUID of the node to be copied/moved")
    public String getPathOrId() {
        return pathOrId;
    }

    @GraphQLField
    @GraphQLName("destParentPathOrId")
    @GraphQLNonNull
    @GraphQLDescription("Path or UUID of the destination parent node to copy/move the node to")
    public String getDestParentPathOrId() {
        return destParentPathOrId;
    }

    @GraphQLField
    @GraphQLName("destName")
    @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved")
    public String getDestName() {
        return destName;
    }
}
