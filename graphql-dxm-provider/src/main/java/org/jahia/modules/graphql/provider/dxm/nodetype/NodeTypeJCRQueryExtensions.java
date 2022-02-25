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

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

@GraphQLTypeExtension(GqlJcrQuery.class)
public class NodeTypeJCRQueryExtensions {

    @GraphQLField
    @GraphQLName("nodeTypeByName")
    @GraphQLDescription("Get a node type by its name")
    public static GqlJcrNodeType getNodeTypeByName(@GraphQLNonNull @GraphQLName("name") @GraphQLDescription("Node type name") String name) {
        try {
            return new GqlJcrNodeType(NodeTypeRegistry.getInstance().getNodeType(name));
        } catch (NoSuchNodeTypeException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("nodeTypesByNames")
    @GraphQLDescription("Get multiple node types by their names")
    public static Collection<GqlJcrNodeType> getNodeTypesByNames(@GraphQLNonNull @GraphQLName("names") @GraphQLDescription("Node type names") Collection<String> names) {
        LinkedHashSet<GqlJcrNodeType> result = new LinkedHashSet<>(names.size());
        for (String name : names) {
            ExtendedNodeType nodeType;
            try {
                nodeType = NodeTypeRegistry.getInstance().getNodeType(name);
            } catch (NoSuchNodeTypeException e) {
                throw new DataFetchingException(e);
            }
            result.add(new GqlJcrNodeType(nodeType));
        }
        return result;
    }

    @GraphQLField
    @GraphQLName("nodeTypes")
    @GraphQLDescription("Get a list of nodetypes based on specified parameter")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public static DXPaginatedData<GqlJcrNodeType> getNodeTypes(@GraphQLName("filter") @GraphQLDescription("Filter on node type") NodeTypesListInput input,
                                                               @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                               DataFetchingEnvironment environment) {
        try {
            PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
            Stream<GqlJcrNodeType> mapped = NodeTypeHelper.getNodeTypes(input).map(GqlJcrNodeType::new)
                    .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));
            return PaginationHelper.paginate(mapped, GqlJcrNodeType::getName, arguments);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
