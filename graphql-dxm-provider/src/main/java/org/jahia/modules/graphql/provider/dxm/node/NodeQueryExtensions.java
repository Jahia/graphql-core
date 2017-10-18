package org.jahia.modules.graphql.provider.dxm.node;

import graphql.ErrorType;
import graphql.annotations.*;
import graphql.servlet.GraphQLQuery;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.QueryLanguage.SQL2;

@GraphQLTypeExtension(GraphQLQuery.class)
public class NodeQueryExtensions {

    public enum QueryLanguage {
        SQL2,
        XPATH
    }

    @GraphQLField
    @GraphQLNonNull
    public static GqlJcrNode getNodeById(@GraphQLName("uuid") @GraphQLNonNull String uuid,
                                         @GraphQLName("workspace") String workspace) throws BaseGqlClientException {
        try {
            return getGqlNodeById(uuid, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    @GraphQLField
    @GraphQLNonNull
    public static GqlJcrNode getNodeByPath(@GraphQLName("path") @GraphQLNonNull String path,
                                           @GraphQLName("workspace") String workspace) throws BaseGqlClientException {
        try {
            return getGqlNodeByPath(path, workspace);
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    @GraphQLField
    @GraphQLNonNull
    public static List<GqlJcrNode> getNodesById(@GraphQLName("uuids") @GraphQLNonNull List<@GraphQLNonNull String> uuids,
                                                @GraphQLName("workspace") String workspace) throws BaseGqlClientException {
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
                                                  @GraphQLName("workspace") String workspace) throws BaseGqlClientException {
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

    @GraphQLField
    @GraphQLNonNull
    @GraphQLConnection
    public static List<GqlJcrNode> getNodesByQuery(@GraphQLName("query") @GraphQLNonNull String query,
                                                   @GraphQLName("queryLanguage") @GraphQLDefaultValue(QueryLanguageDefaultValue.class) QueryLanguage queryLanguage,
                                                   @GraphQLName("workspace") String workspace) throws BaseGqlClientException {
        try {
            List<GqlJcrNode> nodes = new ArrayList<>();
            QueryWrapper q = JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getWorkspace().getQueryManager().createQuery(query, queryLanguage == SQL2 ? Query.JCR_SQL2 : Query.XPATH);
            JCRNodeIteratorWrapper ni = q.execute().getNodes();
            while (ni.hasNext()) {
                JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
                nodes.add(SpecializedTypesHandler.getNode(next));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }
}
