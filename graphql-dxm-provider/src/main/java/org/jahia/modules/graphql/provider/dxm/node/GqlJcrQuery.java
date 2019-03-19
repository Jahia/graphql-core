/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.LocaleUtils;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.QueryManagerWrapper;
import org.jahia.services.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.touk.throwing.exception.WrappedException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.qom.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery.QueryLanguage.SQL2;

/**
 * GraphQL root object for JCR related queries
 */
@GraphQLName("JCRQuery")
@GraphQLDescription("JCR Queries")
public class GqlJcrQuery {

    private static final Logger logger = LoggerFactory.getLogger(GqlJcrQuery.class);

    private NodeQueryExtensions.Workspace workspace;

    public GqlJcrQuery(NodeQueryExtensions.Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * JCR query languages available to use for nodes querying.
     */
    @GraphQLDescription("JCR query languages available to use for nodes querying")
    public enum QueryLanguage {

        /**
         * SQL2 query language.
         */
        @GraphQLDescription("SQL2 query language")
        SQL2(Query.JCR_SQL2),

        /**
         * XPath query language.
         */
        @GraphQLDescription("XPath query language")
        XPATH(Query.XPATH);

        private String jcrQueryLanguage;

        private QueryLanguage(String jcrQueryLanguage) {
            this.jcrQueryLanguage = jcrQueryLanguage;
        }

        /**
         * @return Corresponding language value defined by the JCR API
         */
        public String getJcrQueryLanguage() {
            return jcrQueryLanguage;
        }
    }

    /**
     * @return Get the workspace of the query
     */
    @GraphQLField
    @GraphQLName("workspace")
    @GraphQLNonNull
    @GraphQLDescription("Get the workspace of the query")
    public NodeQueryExtensions.Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Get GraphQL representation of a node by its UUID.
     *
     * @param uuid The UUID of the node
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodeById")
    @GraphQLDescription("Get GraphQL representation of a node by its UUID")
    public GqlJcrNode getNodeById(@GraphQLName("uuid") @GraphQLNonNull @GraphQLDescription("The UUID of the node") String uuid) {
        try {
            return getGqlNodeById(uuid);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representation of a node by its path.
     *
     * @param path The path of the node
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodeByPath")
    @GraphQLDescription("Get GraphQL representation of a node by its path")
    public GqlJcrNode getNodeByPath(@GraphQLName("path") @GraphQLNonNull @GraphQLDescription("The path of the node") String path) {
        try {
            return getGqlNodeByPath(path);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their UUIDs.
     *
     * @param uuids The UUIDs of the nodes
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesById")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their UUIDs")
    public Collection<GqlJcrNode> getNodesById(@GraphQLName("uuids") @GraphQLNonNull @GraphQLDescription("The UUIDs of the nodes") Collection<@GraphQLNonNull String> uuids) {
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(uuids.size());
            for (String uuid : uuids) {
                nodes.add(getGqlNodeById(uuid));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their paths.
     *
     * @param paths The paths of the nodes
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesByPath")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their paths")
    public Collection<GqlJcrNode> getNodesByPath(@GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("The paths of the nodes") Collection<@GraphQLNonNull String> paths) {
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(paths.size());
            for (String path : paths) {
                nodes.add(getGqlNodeByPath(path));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of nodes using a query language supported by JCR.
     *
     * @param query         The query string
     * @param queryLanguage The query language
     * @param language      language to access node properties in
     * @param fieldFilter   Filter by GraphQL field values
     * @param environment   the execution content instance
     * @return GraphQL representations of nodes selected according to the query supplied
     * @throws BaseGqlClientException In case of issues executing the query
     */
    @GraphQLField
    @GraphQLName("nodesByQuery")
    @GraphQLDescription("Get GraphQL representations of nodes using a query language supported by JCR")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNode> getNodesByQuery(
            @GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
            @GraphQLName("queryLanguage") @GraphQLDefaultValue(QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") QueryLanguage queryLanguage,
            @GraphQLName("language") @GraphQLDescription("Language to access node properties in") String language,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
            @GraphQLName("fieldSorter") @GraphQLDescription("sort by GraphQL field values") FieldSorterInput fieldSorter,
            @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields by criteria") FieldGroupingInput fieldGrouping,
            DataFetchingEnvironment environment
    ) {
        try {
            QueryManagerWrapper queryManager = getSession(language).getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            JCRNodeIteratorWrapper nodes = q.execute().getNodes();
            // todo: naive implementation of the pagination, could be improved in some cases by setting limit/offset in query
            return NodeHelper.getPaginatedNodesList(nodes, null, null, null, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of nodes using a criteria object.
     *
     * @param criteria    The criteria to fetch nodes by
     * @param fieldFilter Filter by GraphQL field values
     * @param environment The execution context
     * @return GraphQL representations of nodes fetched
     * @throws BaseGqlClientException In case of issues fetching nodes
     */
    @GraphQLField
    @GraphQLName("nodesByCriteria")
    @GraphQLDescription("handles query nodes with QOM factory")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNode> getNodesByCriteria(
            @GraphQLName("criteria") @GraphQLNonNull @GraphQLDescription("The criteria to fetch nodes by") GqlJcrNodeCriteriaInput criteria,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by GraphQL field values") FieldFiltersInput fieldFilter,
            @GraphQLName("fieldSorter") @GraphQLDescription("sort by GraphQL field values") FieldSorterInput fieldSorter,
            @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields by criteria") FieldGroupingInput fieldGrouping,
            DataFetchingEnvironment environment
    ) {
        try {
            Session session = getSession(criteria.getLanguage());
            QueryObjectModelFactory factory = session.getWorkspace().getQueryManager().getQOMFactory();
            GqlConstraintHandler handler = new GqlConstraintHandler(factory, session.getValueFactory());
            Selector source = factory.selector(criteria.getNodeType(), "node");
            Constraint constraintTree = handler.getConstraintTree(source.getSelectorName(), criteria);
            Ordering ordering = handler.getOrderingByProperty(source.getSelectorName(), criteria);
            QueryObjectModel queryObjectModel = factory.createQuery(source, constraintTree, ordering == null ? null : new Ordering[]{ordering}, null);
            NodeIterator it = queryObjectModel.execute().getNodes();
            return NodeHelper.getPaginatedNodesList(it, null, null, null, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (WrappedException e) {
            Throwable cause = e.getCause();
            while (cause instanceof WrappedException) {
                cause = cause.getCause();
            }
            if (cause instanceof BaseGqlClientException) {
                throw (BaseGqlClientException) cause;
            }
            throw new DataFetchingException(cause);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }


    private GqlJcrNode getGqlNodeByPath(String path) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession().getNode(path));
    }

    private GqlJcrNode getGqlNodeById(String uuid) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession().getNodeByIdentifier(uuid));
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue());
    }

    private JCRSessionWrapper getSession(String language) throws RepositoryException {
        if (language == null) {
            return getSession();
        }
        Locale locale = LocaleUtils.toLocale(language);
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue(), locale);
    }

    public static class QueryLanguageDefaultValue implements Supplier<Object> {

        @Override
        public GqlJcrQuery.QueryLanguage get() {
            return SQL2;
        }
    }

}
