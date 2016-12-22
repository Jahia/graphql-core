package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.tuckey.web.filters.urlrewrite.Run;

import javax.jcr.RepositoryException;
import java.util.List;


class IsNodeTypeDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        if (dataFetchingEnvironment.getSource() instanceof DXGraphQLNode) {
            try {
                List<String> types = dataFetchingEnvironment.getArgument("anyType");
                for (String type : types) {
                    if (((DXGraphQLNode) dataFetchingEnvironment.getSource()).getNode().isNodeType(type)) {
                        return true;
                    }
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
