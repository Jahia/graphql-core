package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLGenericJCRNode;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLJCRNode;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

public  class NamedChildDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = JCRNodeTypeResolver.unescape(StringUtils.substringAfter(name, JCRNodeTypeResolver.CHILD_PREFIX));

        try {
            DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            JCRNodeWrapper child = jcrNodeWrapper.getNode(name);
            return new DXGraphQLGenericJCRNode(child);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
