package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

class NamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = DXGraphQLNodeBuilder.unescape(StringUtils.substringAfter(name, DXGraphQLNodeBuilder.PROPERTY_PREFIX));

        try {
            DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            if (!jcrNodeWrapper.hasProperty(name)) {
                return null;
            }
            Property property = jcrNodeWrapper.getProperty(name);

            if (!property.isMultiple()) {
                return property.getString();
            } else {
                List<String> res = new ArrayList<>();
                for (Value value : property.getValues()) {
                    res.add(value.getString());
                }
                return res;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
