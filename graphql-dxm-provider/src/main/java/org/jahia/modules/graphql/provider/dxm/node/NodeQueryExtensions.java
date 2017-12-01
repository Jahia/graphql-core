package org.jahia.modules.graphql.provider.dxm.node;

import graphql.ErrorType;
import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.*;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.QueryLanguage.SQL2;

/**
 * A query extension that adds a possibility to fetch nodes by their UUIDs, paths, or via an SQL2/Xpath query.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class NodeQueryExtensions {

    /**
     * JCR query languages available to use for nodes querying.
     */
    public enum QueryLanguage {

        /**
         * SQL2 query language.
         */
        SQL2(Query.JCR_SQL2),

        /**
         * XPath query language.
         */
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
     * Get GraphQL representation of a node by its UUID.
     *
     * @param uuid The UUID of the node
     * @param workspace The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get GraphQL representation of a node by its UUID")
    public static GqlJcrNode getNodeById(@GraphQLName("uuid") @GraphQLNonNull @GraphQLDescription("The UUID of the node") String uuid,
                                         @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace)
    throws BaseGqlClientException {
        try {
            return getGqlNodeById(uuid, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    /**
     * Get GraphQL representation of a node by its path.
     *
     * @param path The path of the node
     * @param workspace The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get GraphQL representation of a node by its path")
    public static GqlJcrNode getNodeByPath(@GraphQLName("path") @GraphQLNonNull @GraphQLDescription("The path of the node") String path,
                                           @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either 'default', 'live', or null to use 'default' by default") String workspace)
    throws BaseGqlClientException {
        try {
            return getGqlNodeByPath(path, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their UUIDs.
     *
     * @param uuids The UUIDs of the nodes
     * @param workspace The name of the workspace to fetch the nodes from; either 'default', 'live', or null to use 'default' by default
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their UUIDs")
    public static Collection<GqlJcrNode> getNodesById(@GraphQLName("uuids") @GraphQLNonNull @GraphQLDescription("The UUIDs of the nodes") Collection<@GraphQLNonNull String> uuids,
                                                      @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the nodes from; either 'default', 'live', or null to use 'default' by default") String workspace)
    throws BaseGqlClientException {
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(uuids.size());
            for (String uuid : uuids) {
                nodes.add(getGqlNodeById(uuid, workspace));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their paths.
     *
     * @param paths The paths of the nodes
     * @param workspace The name of the workspace to fetch the nodes from; either 'default', 'live', or null to use 'default' by default
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their paths")
    public static Collection<GqlJcrNode> getNodesByPath(@GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("The paths of the nodes") Collection<@GraphQLNonNull String> paths,
                                                        @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the nodes from; either 'default', 'live', or null to use 'default' by default") String workspace)
    throws BaseGqlClientException {
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(paths.size());
            for (String path : paths) {
                nodes.add(getGqlNodeByPath(path, workspace));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    /**
     * Get GraphQL representations of nodes using a query language supported by JCR.
     *
     * @param query The query string
     * @param queryLanguage The query language
     * @param workspace The name of the workspace to select nodes from; either 'default', 'live', or null to use 'default' by default
     * @return GraphQL representations of nodes selected according to the query supplied
     * @throws BaseGqlClientException In case of issues executing the query
     */
    @GraphQLField
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("Get GraphQL representations of nodes using a query language supported by JCR")
    public static DXPaginatedData<GqlJcrNode> getNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                              @GraphQLName("queryLanguage") @GraphQLDefaultValue(QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") QueryLanguage queryLanguage,
                                                              @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to select nodes from; either 'default', 'live', or null to use 'default' by default") String workspace, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            List<GqlJcrNode> result = new LinkedList<>();
            QueryManagerWrapper queryManager = getSession(workspace).getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            JCRNodeIteratorWrapper nodes = q.execute().getNodes();
            while (nodes.hasNext()) {
                JCRNodeWrapper node = (JCRNodeWrapper) nodes.next();
                result.add(SpecializedTypesHandler.getNode(node));
            }
            // todo: naive implementation of the pagination, could be improved in some cases by setting limit/offset in query
            return PaginationHelper.paginate(result, NodeCursor.getInstance(), environment);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    private static GqlJcrNode getGqlNodeByPath(String path, String workspace) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession(workspace).getNode(path));
    }

    private static GqlJcrNode getGqlNodeById(String uuid, String workspace) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession(workspace).getNodeByIdentifier(uuid));
    }

    private static JCRSessionWrapper getSession(String workspace) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
    }

    public static class QueryLanguageDefaultValue implements Supplier<Object> {

        @Override
        public NodeQueryExtensions.QueryLanguage get() {
            return SQL2;
        }
    }
}
