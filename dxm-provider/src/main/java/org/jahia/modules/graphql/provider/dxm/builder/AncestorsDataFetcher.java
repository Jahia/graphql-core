package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLConnection;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRItemWrapper;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

class AncestorsDataFetcher implements DataFetcher<List<DXGraphQLNode>>, DXGraphQLConnection.CursorFetcher<DXGraphQLNode> {
    @Override
    public List<DXGraphQLNode> get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        List<DXGraphQLNode> ancestors = new ArrayList<DXGraphQLNode>();

        String upToPath = dataFetchingEnvironment.getArgument("upToPath");
        String upToPathSlash = upToPath + "/";

        try {
            List<JCRItemWrapper> jcrAncestors = node.getNode().getAncestors();
            for (JCRItemWrapper ancestor : jcrAncestors) {
                if (upToPath == null || ancestor.getPath().equals(upToPath) || ancestor.getPath().startsWith(upToPathSlash)) {
                    ancestors.add(new DXGraphQLNode((JCRNodeWrapper) ancestor));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return ancestors;
    }

    @Override
    public String getCursor(DXGraphQLNode node) {
        return node.getIdentifier();
    }
}
