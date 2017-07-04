package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLGenericJCRNode;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLJCRNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.ArrayList;
import java.util.List;

public class UnnamedChildNodesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = JCRNodeTypeResolver.unescape(StringUtils.substringAfter(type, JCRNodeTypeResolver.UNNAMED_CHILD_PREFIX));

        DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
        List<DXGraphQLJCRNode> results = new ArrayList<>();
        JCRNodeWrapper jcrNodeWrapper = node.getNode();
        for (JCRNodeWrapper n : JCRContentUtils.getChildrenOfType(jcrNodeWrapper, type)) {
            results.add(new DXGraphQLGenericJCRNode(n, type));
        }

        return results;
    }
}
