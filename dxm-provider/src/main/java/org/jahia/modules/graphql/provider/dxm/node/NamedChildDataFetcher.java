package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

public  class NamedChildDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = SpecializedTypesHandler.unescape(StringUtils.substringAfter(name, SpecializedTypesHandler.CHILD_PREFIX));

        try {
            DXGraphQLJCRNode node = dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            JCRNodeWrapper child = jcrNodeWrapper.getNode(name);
            return SpecializedTypesHandler.getNode(child);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
