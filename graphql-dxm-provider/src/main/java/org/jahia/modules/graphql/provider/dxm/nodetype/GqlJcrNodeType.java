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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.connection.GraphQLConnection;
import graphql.annotations.connection.PaginatedData;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GraphQL representation of a JCR node type
 */
@GraphQLName("JCRNodeType")
@GraphQLDescription("GraphQL representation of a JCR node type")
public class GqlJcrNodeType {
    public static final Logger logger = LoggerFactory.getLogger(GqlJcrNodeType.class);

    private ExtendedNodeType nodeType;


    public GqlJcrNodeType(ExtendedNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public ExtendedNodeType getNodeType() {
        return nodeType;
    }

    @GraphQLField()
    @GraphQLName("name")
    @GraphQLDescription("Node type name")
    public String getName() {
        return nodeType.getName();
    }

    @GraphQLField()
    @GraphQLName("displayName")
    @GraphQLDescription("Node type displayable name")
    public String getDisplayName(@GraphQLName("language") @GraphQLDescription("Language") @GraphQLNonNull String language) {
        return nodeType.getLabel(LanguageCodeConverters.languageCodeToLocale(language));
    }

    @GraphQLField()
    @GraphQLName("icon")
    @GraphQLDescription("Node type icon")
    public String getIcon() {
        try {
            return getIcon(nodeType);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLName("systemId")
    @GraphQLDescription("System ID of the node type, corresponding to the name of the module declaring it.")
    public String getSystemId() {
        return nodeType.getSystemId();
    }

    @GraphQLField
    @GraphQLName("mixin")
    @GraphQLDescription("Returns true if this is a mixin type; returns false otherwise.")
    public boolean isMixin() {
        return nodeType.isMixin();
    }

    @GraphQLField
    @GraphQLName("abstract")
    @GraphQLDescription("Returns true if this is an abstract node type; returns false otherwise.")
    public boolean isAbstract() {
        return nodeType.isAbstract();
    }

    @GraphQLField
    @GraphQLName("hasOrderableChildNodes")
    @GraphQLDescription("Returns true if nodes of this type must support orderable child nodes; returns false otherwise.")
    public boolean isHasOrderableChildNodes() {
        return nodeType.hasOrderableChildNodes();
    }

    @GraphQLField
    @GraphQLName("queryable")
    @GraphQLDescription("Returns true if the node type is queryable.")
    public boolean isQueryable() {
        return nodeType.isQueryable();
    }

    @GraphQLField
    @GraphQLName("isNodeType")
    @GraphQLDescription("Reports if the current node type matches the nodetype(s) passed in parameter")
    @GraphQLNonNull
    public boolean isNodeType(@GraphQLName("type") @GraphQLDescription("Node type name") @GraphQLNonNull GqlJcrNode.NodeTypesInput input) {
        return NodeTypeHelper.getTypesPredicate(input).test(nodeType);
    }

    @GraphQLField
    @GraphQLName("primaryItem")
    @GraphQLDescription("Returns the name of the primary item (one of the child items of the nodes of this node type). If this node has no primary item, then this method null.")
    public GqlJcrItemDefinition getPrimaryItem() {
        String primaryItemName = nodeType.getPrimaryItemName();
        if (primaryItemName != null) {
            if (nodeType.getChildNodeDefinitionsAsMap().containsKey(primaryItemName)) {
                return new GqlJcrNodeDefinition(nodeType.getChildNodeDefinitionsAsMap().get(primaryItemName));
            }
            if (nodeType.getPropertyDefinitionsAsMap().containsKey(primaryItemName)) {
                return new GqlJcrPropertyDefinition(nodeType.getPropertyDefinitionsAsMap().get(primaryItemName));
            }
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("properties")
    @GraphQLDescription("Returns an array containing the property definitions of this node type.")
    public List<GqlJcrPropertyDefinition> getProperties(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        return Arrays.stream(nodeType.getPropertyDefinitions())
                .map(GqlJcrPropertyDefinition::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("nodes")
    @GraphQLDescription("Returns an array containing the child node definitions of this node type.")
    public List<GqlJcrNodeDefinition> getNodes(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        return Arrays.stream(nodeType.getChildNodeDefinitions())
                .map(GqlJcrNodeDefinition::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("subTypes")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("Returns all subtypes of this node type in the node type inheritance hierarchy.")
    public PaginatedData<GqlJcrNodeType> getSubtypes(DataFetchingEnvironment environment) {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        Stream<GqlJcrNodeType> subTypes = nodeType.getSubtypesAsList().stream().map(GqlJcrNodeType::new);
        return PaginationHelper.paginate(subTypes, t -> PaginationHelper.encodeCursor(t.getName()), arguments);
    }

    @GraphQLField
    @GraphQLName("supertypes")
    @GraphQLDescription("Returns all supertypes of this node type in the node type inheritance hierarchy.")
    public List<GqlJcrNodeType> getSupertypes(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        return nodeType.getSupertypeSet().stream()
                .map(GqlJcrNodeType::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forList(environment)))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("extends")
    @GraphQLDescription("Returns the node types dynamically extended by the current node type (in CND using the `extends=...` syntax), filtered by the specified parameters.")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNodeType> extendsNodes(@GraphQLName("filter") @GraphQLDescription("Filter on node type") NodeTypesListInput input,
                                                        @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                        DataFetchingEnvironment environment) {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        Stream<GqlJcrNodeType> mapped = nodeType.getMixinExtends().stream()
                .filter(NodeTypeHelper.getFilterPredicate(input))
                .map(GqlJcrNodeType::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));
        return PaginationHelper.paginate(mapped, GqlJcrNodeType::getName, arguments);
    }

    @GraphQLField
    @GraphQLName("extendedBy")
    @GraphQLDescription("Returns the node types that extend the current node type (in CND using the `extends=...` syntax), filtered by the specified parameters.")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNodeType> extendedByNodes(@GraphQLName("filter") @GraphQLDescription("Filter on node type") NodeTypesListInput input,
                                                           @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                           DataFetchingEnvironment environment) {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        NodeTypeRegistry registry = NodeTypeRegistry.getInstance();
        Set<ExtendedNodeType> extendedNodeTypes = Optional.ofNullable(registry.getMixinExtensions().get(nodeType))
                .orElse(Collections.emptySet());
        Stream<GqlJcrNodeType> mapped = extendedNodeTypes.stream()
                .filter(NodeTypeHelper.getFilterPredicate(input))
                .map(GqlJcrNodeType::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));
        return PaginationHelper.paginate(mapped, GqlJcrNodeType::getName, arguments);
    }

    private String getIcon(ExtendedNodeType type) throws RepositoryException {
        return JCRContentUtils.getIconWithContext(type);
    }
}
