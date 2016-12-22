package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;

import javax.jcr.RepositoryException;

class PrimaryNodeTypeDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (environment.getSource() instanceof DXGraphQLNode) {
            try {
                return new DXGraphQLNodeType(((DXGraphQLNode) environment.getSource()).getNode().getPrimaryNodeType());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
