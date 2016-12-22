package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.ArrayList;
import java.util.List;

class UnnamedChildNodesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = DXGraphQLNodeBuilder.unescape(StringUtils.substringAfter(type, DXGraphQLNodeBuilder.UNNAMED_CHILD_PREFIX));

        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        List<DXGraphQLNode> results = new ArrayList<>();
        JCRNodeWrapper jcrNodeWrapper = node.getNode();
        for (JCRNodeWrapper n : JCRContentUtils.getChildrenOfType(jcrNodeWrapper, type)) {
            results.add(new DXGraphQLNode(n, type));
        }

        return results;
    }
}
