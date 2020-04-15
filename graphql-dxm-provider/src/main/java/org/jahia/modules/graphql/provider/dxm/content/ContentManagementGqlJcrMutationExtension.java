/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrReproducibleNodeInput;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;

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
     * @param namingConflictResolutionStrategy The way to deal with duplicate node names when they are not allowed
     * @return Mutation object representing the node at the new location
     */
    @GraphQLField
    @GraphQLDescription("Paste a single node to a different parent node")
    public GqlJcrNodeMutation pasteNode(
        @GraphQLName("mode") @GraphQLNonNull @GraphQLDescription("Paste mode, either COPY or MOVE") PasteMode mode,
        @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be pasted") String pathOrId,
        @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to paste the node to") String destParentPathOrId,
        @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName,
        @GraphQLName("namingConflictResolution") @GraphQLDefaultValue(SupplierFail.class) @GraphQLDescription("The way to deal with duplicate node names when they are not allowed, either FAIL or RENAME") NodeNamingConflictResolutionStrategy namingConflictResolution
    ) throws BaseGqlClientException {
        destName = getNodeName(pathOrId, destParentPathOrId, destName, namingConflictResolution);
        if (mode == PasteMode.COPY) {
            return mutation.copyNode(pathOrId, destParentPathOrId, destName);
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
     * @param namingConflictResolutionStrategy The way to deal with duplicate node names when they are not allowed
     * @return A collection of mutation objects representing pasted nodes at their new location(s)
     */
    @GraphQLField
    @GraphQLDescription("Paste multiple nodes to different parent node(s)")
    public Collection<GqlJcrNodeMutation> pasteNodes(
        @GraphQLName("mode") @GraphQLNonNull @GraphQLDescription("Paste mode, either COPY or MOVE") PasteMode mode,
        @GraphQLName("nodes") @GraphQLNonNull  @GraphQLDescription("Info about nodes to paste and their new parent node(s)") Collection<@GraphQLNonNull GqlJcrReproducibleNodeInput> nodes,
        @GraphQLName("namingConflictResolution") @GraphQLDefaultValue(SupplierFail.class) @GraphQLDescription("The way to deal with duplicate node names when they are not allowed, either FAIL or RENAME") NodeNamingConflictResolutionStrategy namingConflictResolution
    ) throws BaseGqlClientException {
        ArrayList<GqlJcrNodeMutation> result = new ArrayList<>(nodes.size());
        for (GqlJcrReproducibleNodeInput node : nodes) {
            result.add(pasteNode(mode, node.getPathOrId(), node.getDestParentPathOrId(), node.getDestName(), namingConflictResolution));
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
