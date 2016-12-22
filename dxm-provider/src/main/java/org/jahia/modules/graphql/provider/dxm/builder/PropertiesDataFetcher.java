package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLProperty;
import org.jahia.services.content.JCRPropertyWrapper;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

class PropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        List<DXGraphQLProperty> propertyList = new ArrayList<DXGraphQLProperty>();
        try {
            List<String> names = dataFetchingEnvironment.getArgument("names");
            if (names != null && !names.isEmpty()) {
                for (String name : names) {
                    if (node.getNode().hasProperty(name)) {
                        propertyList.add(new DXGraphQLProperty(node.getNode().getProperty(name)));
                    }
                }
            } else {
                PropertyIterator pi = node.getNode().getProperties();
                while (pi.hasNext()) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) pi.nextProperty();
                    propertyList.add(new DXGraphQLProperty(property));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return propertyList;
    }
}
