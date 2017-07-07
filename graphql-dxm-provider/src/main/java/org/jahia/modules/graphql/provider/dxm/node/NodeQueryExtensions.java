package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.*;
import graphql.servlet.GraphQLQuery;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
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
        SQL2, XPATH
    }

    @GraphQLField
    public static DXGraphQLJCRNode getNodeById(@GraphQLNonNull @GraphQLName("uuid") String uuid,
                                               @GraphQLName("workspace") String workspace) {
        try {
            return SpecializedTypesHandler.getNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(uuid));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public static DXGraphQLJCRNode getNodeByPath(@GraphQLNonNull @GraphQLName("path") String path,
                                                 @GraphQLName("workspace") String workspace) {
        try {
            return SpecializedTypesHandler.getNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public static class QueryLanguageDefaultValue implements Supplier<Object> {
        @Override
        public NodeQueryExtensions.QueryLanguage get() {
            return SQL2;
        }
    }

    @GraphQLField
    public static List<DXGraphQLJCRNode> getNodesByQuery(@GraphQLNonNull @GraphQLName("query") String query,
                                                          @GraphQLDefaultValue(QueryLanguageDefaultValue.class) @GraphQLName("queryLanguage") QueryLanguage queryLanguage,
                                                          @GraphQLName("workspace") String workspace) {
        try {
            List<DXGraphQLJCRNode> nodes = new ArrayList<>();
            QueryWrapper q = JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getWorkspace().getQueryManager().createQuery(query, queryLanguage == SQL2 ? Query.JCR_SQL2 : Query.XPATH);
            JCRNodeIteratorWrapper ni = q.execute().getNodes();
            while (ni.hasNext()) {
                JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
                nodes.add(SpecializedTypesHandler.getNode(next));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

}
