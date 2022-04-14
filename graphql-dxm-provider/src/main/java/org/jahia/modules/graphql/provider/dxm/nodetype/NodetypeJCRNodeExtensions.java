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
import java.util.*;
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

    private static void collectSubTypes(Collection<ExtendedNodeType> result, ExtendedNodeType type) {
        for (NodeTypeIterator it = type.getSubtypes(); it.hasNext(); ) {
            ExtendedNodeType subType = (ExtendedNodeType) it.next();
            result.add(subType);
            collectSubTypes(result, subType);
        }
    }

    @GraphQLField
    @GraphQLName("primaryNodeType")
    @GraphQLDescription("Get the primary node type of this node")
    @GraphQLNonNull
    public GqlJcrNodeType getPrimaryNodeType() {
        try {
            return new GqlJcrNodeType(node.getNode().getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLName("isNodeType")
    @GraphQLDescription("Reports if the current node matches the nodetype(s) passed in parameter")
    @GraphQLNonNull
    public boolean isNodeType(@GraphQLName("type") @GraphQLDescription("Node type name") @GraphQLNonNull GqlJcrNode.NodeTypesInput input) {
        return NodeHelper.getTypesPredicate(input).test(node.getNode());
    }

    @GraphQLField
    @GraphQLName("mixinTypes")
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
    @GraphQLName("definition")
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
    @GraphQLName("allowedChildNodeTypes")
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
}
