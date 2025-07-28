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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.connection.GraphQLConnection;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.LocaleUtils;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintHandler;
import org.jahia.modules.graphql.provider.dxm.JahiaDataFetchingExceptionHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldGroupingInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.QueryManagerWrapper;
import org.jahia.services.query.QueryWrapper;
import pl.touk.throwing.exception.WrappedException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery.QueryLanguage.SQL2;

/**
 * GraphQL root object for JCR related queries
 */
@GraphQLName(GqlJcrQuery.NAME)
@GraphQLDescription("JCR Queries")
public class GqlJcrQuery {

    public static final String NAME = "JCRQuery";
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
        @GraphQLDescription("SQL2 query language") SQL2(Query.JCR_SQL2),

        /**
         * XPath query language.
         */
        @GraphQLDescription("XPath query language") XPATH(Query.XPATH);

        private String jcrQueryLanguage;

        QueryLanguage(String jcrQueryLanguage) {
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
     * @param validInLanguage Check node validity in this language
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLName("nodeById")
    @GraphQLDescription("Get GraphQL representation of a node by its UUID")
    public GqlJcrNode getNodeById(
            @GraphQLName("uuid") @GraphQLNonNull @GraphQLDescription("The UUID of the node") String uuid,
            @GraphQLName("validInLanguage") @GraphQLDescription("Check node validity in this language. \n\n Node validity is determined by multiple conditions: \n\n * The node exists \n * The node is not attached to a visibility condition preventing its display \n * The node is published in this language \n * The node's language has not been invalidated.") String validInLanguage
    ) {
        try {
            return getGqlNodeById(uuid, validInLanguage);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representation of a node by its path.
     *
     * @param path The path of the node
     * @param validInLanguage Check node validity in this language
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLName("nodeByPath")
    @GraphQLDescription("Get GraphQL representation of a node by its path")
    public GqlJcrNode getNodeByPath(
            @GraphQLName("path") @GraphQLNonNull @GraphQLDescription("The path of the node") String path,
            @GraphQLName("validInLanguage") @GraphQLDescription("Check node validity in this language. \n\n Node validity is determined by multiple conditions: \n\n * The node exists \n * The node is not attached to a visibility condition preventing its display \n * The node is published in this language \n * The node's language has not been invalidated.") String validInLanguage) {
        try {
            return getGqlNodeByPath(path, validInLanguage);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their UUIDs.
     *
     * @param uuids The UUIDs of the nodes
     * @param validInLanguage Check node validity in this language
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesById")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their UUIDs")
    public DataFetcherResult<Collection<GqlJcrNode>> getNodesById(
            @GraphQLName("uuids") @GraphQLNonNull @GraphQLDescription("The UUIDs of the nodes") Collection<@GraphQLNonNull String> uuids,
            @GraphQLName("validInLanguage") @GraphQLDescription("Check node validity in this language. \n\n Node validity is determined by multiple conditions: \n\n * The node exists \n * The node is not attached to a visibility condition preventing its display \n * The node is published in this language \n * The node's language has not been invalidated.") String validInLanguage,
            DataFetchingEnvironment environment) {
        List<GqlJcrNode> nodes = new ArrayList<>(uuids.size());
        DataFetcherResult.Builder<Collection<GqlJcrNode>> result = DataFetcherResult.newResult();

        for (String uuid : uuids) {
            try {
                nodes.add(getGqlNodeById(uuid, validInLanguage));
            } catch (RepositoryException re) {
                result.error(JahiaDataFetchingExceptionHandler.transformException(new DataFetchingException(re), environment));
            }
        }

        return result.data(nodes).build();
    }

    /**
     * Get GraphQL representations of multiple nodes by their paths.
     *
     * @param paths The paths of the nodes
     * @param validInLanguage Check node validity in this language
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesByPath")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their paths")
    public DataFetcherResult<Collection<GqlJcrNode>> getNodesByPath(
            @GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("The paths of the nodes") Collection<@GraphQLNonNull String> paths,
            @GraphQLName("validInLanguage") @GraphQLDescription("Check node validity in this language. \n\n Node validity is determined by multiple conditions: \n\n * The node exists \n * The node is not attached to a visibility condition preventing its display \n * The node is published in this language \n * The node's language has not been invalidated.") String validInLanguage,
            DataFetchingEnvironment environment) {
        List<GqlJcrNode> nodes = new ArrayList<>(paths.size());
        DataFetcherResult.Builder<Collection<GqlJcrNode>> result = DataFetcherResult.newResult();

        for (String path : paths) {
            try {
                nodes.add(getGqlNodeByPath(path, validInLanguage));
            } catch (RepositoryException re) {
                result.error(JahiaDataFetchingExceptionHandler.transformException(new DataFetchingException(re), environment));
            }
        }

        return result.data(nodes).build();
    }

    /**
     * Get GraphQL representations of nodes using a query language supported by JCR.
     *
     * @param query         The query string
     * @param queryLanguage The query language
     * @param language      Language to use for the query
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
            @GraphQLName("language") @GraphQLDescription("Language to use for the query") String language,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
            @GraphQLName("fieldSorter") @GraphQLDescription("Sort by GraphQL field values") FieldSorterInput fieldSorter,
            @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields by criteria") FieldGroupingInput fieldGrouping,
            DataFetchingEnvironment environment) {
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
            DataFetchingEnvironment environment) {
        try {
            Session session = getSession(criteria.getLanguage());
            QueryObjectModelFactory factory = session.getWorkspace().getQueryManager().getQOMFactory();
            GqlConstraintHandler handler = new GqlConstraintHandler(factory, session.getValueFactory());
            Selector source = factory.selector(criteria.getNodeType(), "node");
            Constraint constraintTree = handler.getConstraintTree(source.getSelectorName(), criteria);
            Ordering ordering = handler.getOrderingByProperty(source.getSelectorName(), criteria);
            QueryObjectModel queryObjectModel = factory
                    .createQuery(source, constraintTree, ordering == null ? null : new Ordering[]{ordering}, null);
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

    private GqlJcrNode getGqlNodeByPath(String path, String validInLanguage) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession(validInLanguage).getNode(JCRContentUtils.escapeNodePath(path)));
    }

    private GqlJcrNode getGqlNodeById(String uuid, String validInLanguage) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession(validInLanguage).getNodeByIdentifier(uuid));
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
