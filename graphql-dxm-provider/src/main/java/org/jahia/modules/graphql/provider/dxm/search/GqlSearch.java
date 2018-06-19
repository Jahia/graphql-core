/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.search;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import java.util.LinkedList;
import java.util.List;

/**
 * GraphQL root object for JCR related search queries
 *
 * @author yousria
 */

@GraphQLName("searchQuery")
@GraphQLDescription("Search Queries")
public class GqlSearch {

    private NodeQueryExtensions.Workspace workspace;

    /**
     * Initializes an instance of this class with the specified JCR workspace name.
     * @param workspace the name of the JCR workspace
     */
    public GqlSearch(NodeQueryExtensions.Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     *
     * @param queryInput object containing query criteria
     * @param environment the execution content instance
     * @return GraphQL representations of nodes selected according to the query supplied
     */
    @GraphQLField
    @GraphQLDescription("handles query nodes with QOM factory")
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNode> contentByQuery(@GraphQLName("queryInput") @GraphQLNonNull @GraphQLDescription("query input object")
            JCRNodesQueryInput queryInput, @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values")
            FieldFiltersInput fieldFilter, DataFetchingEnvironment environment){

        return contentByQuery(queryInput, environment, fieldFilter);
    }

    private DXPaginatedData<GqlJcrNode> contentByQuery(JCRNodesQueryInput queryInput, DataFetchingEnvironment environment,
            FieldFiltersInput fieldFilter){
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        List<GqlJcrNode> result = new LinkedList<>();
        try {
            QueryManager queryManager = getSession().getWorkspace().getQueryManager();
            QueryObjectModelFactory factory = queryManager.getQOMFactory();
            Selector source = factory.selector(queryInput.getNodeType(), "nodeType");
            //orderings and constraints are not used for now, TODO with BACKLOG-8027
            QueryObjectModel queryObjectModel = factory.createQuery(source, null, null, null);
            NodeIterator res = queryObjectModel.execute().getNodes();
            while(res.hasNext()){
                JCRNodeWrapper node = (JCRNodeWrapper)res.nextNode();
                if (PermissionHelper.hasPermission(node, environment)) {
                    result.add(SpecializedTypesHandler.getNode(node));
                }
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return PaginationHelper.paginate(FilterHelper.filterConnection(result, fieldFilter, environment), n -> PaginationHelper.encodeCursor(n.getUuid()), arguments);

    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue());
    }



}
