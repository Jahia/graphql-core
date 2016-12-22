package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

class NamedChildDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = DXGraphQLNodeBuilder.unescape(StringUtils.substringAfter(name, DXGraphQLNodeBuilder.CHILD_PREFIX));

        try {
            DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            JCRNodeWrapper child = jcrNodeWrapper.getNode(name);
            return new DXGraphQLNode(child);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
