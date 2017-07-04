package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLJCRNode;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

public class NamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = JCRNodeTypeResolver.unescape(StringUtils.substringAfter(name, JCRNodeTypeResolver.PROPERTY_PREFIX));

        try {
            DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
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
