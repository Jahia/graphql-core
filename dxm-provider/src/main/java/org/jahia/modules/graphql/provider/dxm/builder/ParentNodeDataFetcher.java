package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLProperty;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;

class ParentNodeDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        try {
            return new DXGraphQLNode(node.getNode().getParent());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
