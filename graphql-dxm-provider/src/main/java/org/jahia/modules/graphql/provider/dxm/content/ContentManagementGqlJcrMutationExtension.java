/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrReproducibleNodeInput;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;

/**
 * Provides for content management related mutation extensions.
 */
@GraphQLTypeExtension(GqlJcrMutation.class)
@GraphQLDescription("Content management related extensions of JCR mutations")
public class ContentManagementGqlJcrMutationExtension {

    private GqlJcrMutation mutation;

    /**
     * Whether a node should be copied or moved from its original destination to new one when pasting it.
     */
    public enum PasteMode {
        COPY,
        MOVE
    }

    /**
     * The way to deal with duplicate node names when pasting a node and same name siblings are not allowed.
     */
    public enum NodeNamingConflictResolutionStrategy {
        FAIL,
        RENAME
    }

    public ContentManagementGqlJcrMutationExtension(GqlJcrMutation mutation) {
        this.mutation = mutation;
    }

    /**
     * Paste a single node to a different parent node.
     *
     * @param mode Paste mode
     * @param pathOrId Path or UUID of the node to be pasted
     * @param destParentPathOrId Path or UUID of the destination parent node to paste the node to
     * @param destName The name of the node at the new location or null if its current name should be preserved
     * @param namingConflictResolution The way to deal with duplicate node names when they are not allowed
     * @return Mutation object representing the node at the new location
     */
    @GraphQLField
    @GraphQLDescription("Paste a single node to a different parent node")
    public GqlJcrNodeMutation pasteNode(
        @GraphQLName("mode") @GraphQLNonNull @GraphQLDescription("Paste mode, either COPY or MOVE") PasteMode mode,
        @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be pasted") String pathOrId,
        @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to paste the node to") String destParentPathOrId,
        @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName,
        @GraphQLName("childNodeTypesToSkip") @GraphQLDescription("The child node types that should be skipped during copy") List<String> childNodeTypesToSkip,
        @GraphQLName("namingConflictResolution") @GraphQLDefaultValue(SupplierFail.class) @GraphQLDescription("The way to deal with duplicate node names when they are not allowed, either FAIL or RENAME") NodeNamingConflictResolutionStrategy namingConflictResolution
    ) throws BaseGqlClientException {
        destName = getNodeName(pathOrId, destParentPathOrId, destName, namingConflictResolution);
        if (mode == PasteMode.COPY) {
            return mutation.copyNode(pathOrId, destParentPathOrId, destName, childNodeTypesToSkip);
        } else if (mode == PasteMode.MOVE) {
            return mutation.moveNode(pathOrId, destParentPathOrId, destName);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Paste multiple nodes to different parent node(s).
     *
     * @param mode Paste mode
     * @param nodes Info about nodes to paste and their new parent node(s)
     * @param namingConflictResolution The way to deal with duplicate node names when they are not allowed
     * @return A collection of mutation objects representing pasted nodes at their new location(s)
     */
    @GraphQLField
    @GraphQLDescription("Paste multiple nodes to different parent node(s)")
    public Collection<GqlJcrNodeMutation> pasteNodes(
        @GraphQLName("mode") @GraphQLNonNull @GraphQLDescription("Paste mode, either COPY or MOVE") PasteMode mode,
        @GraphQLName("nodes") @GraphQLNonNull  @GraphQLDescription("Info about nodes to paste and their new parent node(s)") Collection<@GraphQLNonNull GqlJcrReproducibleNodeInput> nodes,
        @GraphQLName("childNodeTypesToSkip") @GraphQLDescription("The child node types that should be skipped during copy") List<String> childNodeTypesToSkip,
        @GraphQLName("namingConflictResolution") @GraphQLDefaultValue(SupplierFail.class) @GraphQLDescription("The way to deal with duplicate node names when they are not allowed, either FAIL or RENAME") NodeNamingConflictResolutionStrategy namingConflictResolution
    ) throws BaseGqlClientException {
        ArrayList<GqlJcrNodeMutation> result = new ArrayList<>(nodes.size());
        for (GqlJcrReproducibleNodeInput node : nodes) {
            result.add(pasteNode(mode, node.getPathOrId(), node.getDestParentPathOrId(), node.getDestName(), childNodeTypesToSkip, namingConflictResolution));
        }
        return result;
    }

    private String getNodeName(String pathOrId, String destParentPathOrId, String destName, NodeNamingConflictResolutionStrategy namingConflictResolution) {

        JCRSessionWrapper session = mutation.getSession();

        if (destName == null) {
            JCRNodeWrapper node = GqlJcrMutation.getNodeFromPathOrId(session, pathOrId);
            destName = node.getName();
        }

        if (namingConflictResolution == NodeNamingConflictResolutionStrategy.RENAME) {
            JCRNodeWrapper destParentNode = GqlJcrMutation.getNodeFromPathOrId(session, destParentPathOrId);
            destName = JCRContentUtils.findAvailableNodeName(destParentNode, destName);
        }

        return destName;
    }

    /**
     * Supplies for NodeNamingConflictResolutionStrategy default value.
     */
    public static class SupplierFail implements Supplier<Object> {

        @Override
        public NodeNamingConflictResolutionStrategy get() {
            return NodeNamingConflictResolutionStrategy.FAIL;
        }
    }
}
