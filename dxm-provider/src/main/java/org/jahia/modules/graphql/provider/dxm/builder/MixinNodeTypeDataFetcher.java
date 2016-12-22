package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;
import org.jahia.services.content.nodetypes.ExtendedNodeType;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

class MixinNodeTypeDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (environment.getSource() instanceof DXGraphQLNode) {
            try {
                List<DXGraphQLNodeType> result = new ArrayList<>();
                ExtendedNodeType[] mixinNodeTypes = ((DXGraphQLNode) environment.getSource()).getNode().getMixinNodeTypes();
                for (ExtendedNodeType nodeType : mixinNodeTypes) {
                    result.add(new DXGraphQLNodeType(nodeType));
                }
                return result;
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
