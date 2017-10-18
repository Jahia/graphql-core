package org.jahia.modules.graphql.provider.dxm.node;

import graphql.ErrorType;
import graphql.annotations.*;
import graphql.servlet.GraphQLQuery;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.QueryManagerWrapper;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.QueryLanguage.SQL2;

@GraphQLTypeExtension(GraphQLQuery.class)
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

    @GraphQLField
    @GraphQLNonNull
    public static GqlJcrNode getNodeById(@GraphQLName("uuid") @GraphQLNonNull String uuid,
                                         @GraphQLName("workspace") String workspace)
    throws BaseGqlClientException {
        try {
            return getGqlNodeById(uuid, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    @GraphQLField
    @GraphQLNonNull
    public static GqlJcrNode getNodeByPath(@GraphQLName("path") @GraphQLNonNull String path,
                                           @GraphQLName("workspace") String workspace)
    throws BaseGqlClientException {
        try {
            return getGqlNodeByPath(path, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    @GraphQLField
    @GraphQLNonNull
    public static List<GqlJcrNode> getNodesById(@GraphQLName("uuids") @GraphQLNonNull List<@GraphQLNonNull String> uuids,
                                                @GraphQLName("workspace") String workspace)
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

    @GraphQLField
    @GraphQLNonNull
    public static List<GqlJcrNode> getNodesByPath(@GraphQLName("paths") @GraphQLNonNull List<@GraphQLNonNull String> paths,
                                                  @GraphQLName("workspace") String workspace)
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
    @GraphQLConnection
    @GraphQLDescription("Get GraphQL representations of nodes using a query language supported by JCR")
    public static List<GqlJcrNode> getNodesByQuery(@GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
                                                   @GraphQLName("queryLanguage") @GraphQLDefaultValue(QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") QueryLanguage queryLanguage,
                                                   @GraphQLName("workspace") @GraphQLDescription("The name of the workspace to select nodes from; either 'default', 'live', or null to use 'default' by default") String workspace)
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
            return result;
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
