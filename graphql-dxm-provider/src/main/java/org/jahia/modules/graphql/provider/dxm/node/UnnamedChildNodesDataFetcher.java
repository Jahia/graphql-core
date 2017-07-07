package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class UnnamedChildNodesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = SpecializedTypesHandler.unescape(StringUtils.substringAfter(type, SpecializedTypesHandler.UNNAMED_CHILD_PREFIX));

        DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
        List<DXGraphQLJCRNode> results = new ArrayList<>();
        JCRNodeWrapper jcrNodeWrapper = node.getNode();
        for (JCRNodeWrapper n : JCRContentUtils.getChildrenOfType(jcrNodeWrapper, type)) {
            try {
                results.add(SpecializedTypesHandler.getNode(n));
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        return results;
    }
}
