/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.nodetype;

import com.google.common.base.Splitter;
import graphql.annotations.annotationTypes.*;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.services.content.nodetypes.ConstraintsHelper;
import org.jahia.services.content.nodetypes.ExtendedNodeDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeIterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extensions for JCRNode.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Extensions for JCRNode")
public class NodetypeJCRNodeExtensions {

    private GqlJcrNode node;

    public NodetypeJCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLNonNull
    public GqlJcrNodeType getPrimaryNodeType() {
        try {
            return new GqlJcrNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Reports if the current node matches the nodetype(s) passed in parameter")
    @GraphQLNonNull
    public boolean getIsNodeType(@GraphQLName("type") @GraphQLNonNull GqlJcrNode.NodeTypesInput input) {
        return NodeHelper.getTypesPredicate(input).test(node.getNode());
    }

    @GraphQLField
    @GraphQLDescription("Returns an array of <code>NodeType</code> objects representing the mixin node types in effect for this node.")
    @GraphQLNonNull
    public List<GqlJcrNodeType> getMixinTypes(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by GraphQL fields values") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        try {
            return Arrays.stream(node.getNode().getMixinNodeTypes())
                    .map(GqlJcrNodeType::new)
                    .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
                    .collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Returns the node definition that applies to this node.")
    public GqlJcrNodeDefinition getDefinition() {
        ExtendedNodeDefinition definition;

        try {
            definition = node.getNode().getDefinition();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        if (definition != null) {
            return new GqlJcrNodeDefinition(definition);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Returns a list of types allowed under the provided node")
    public List<GqlJcrNodeType> getAllowedChildNodeTypes(
        @GraphQLName("includeSubTypes") @GraphQLDescription("Whether all sub-types of allowed child node types should be included") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) boolean includeSubTypes,
        @GraphQLName("fieldFilter") @GraphQLDescription("Filter by GraphQL fields values") FieldFiltersInput fieldFilter,
        DataFetchingEnvironment environment
    ) {

        // TODO: update to invoke the ConstraintsHelper.getConstraintSet and avoid splitting the string.

        String constraints;
        try {
            constraints = ConstraintsHelper.getConstraints(node.getNode());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        if (constraints.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<ExtendedNodeType> types = new LinkedHashSet<>();
        List<String> typeNames = Splitter.on(" ").splitToList(constraints);
        for (String typeName : typeNames) {
            ExtendedNodeType type;
            try {
                type = NodeTypeRegistry.getInstance().getNodeType(typeName);
            } catch (NoSuchNodeTypeException e) {
                throw new RuntimeException(e);
            }
            types.add(type);
            if (includeSubTypes) {
                collectSubTypes(types, type);
            }
        }

        return types
            .stream()
            .map(GqlJcrNodeType::new)
            .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
            .collect(Collectors.toList());
    }

    private static void collectSubTypes(Collection<ExtendedNodeType> result, ExtendedNodeType type) {
        for (NodeTypeIterator it = type.getSubtypes(); it.hasNext(); ) {
            ExtendedNodeType subType = (ExtendedNodeType) it.next();
            result.add(subType);
            collectSubTypes(result, subType);
        }
    }
}
