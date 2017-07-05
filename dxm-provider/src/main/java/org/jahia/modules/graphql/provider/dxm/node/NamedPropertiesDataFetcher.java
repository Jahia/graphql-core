package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class NamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = SpecializedTypesHandler.unescape(StringUtils.substringAfter(name, SpecializedTypesHandler.PROPERTY_PREFIX));

        try {
            DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            if (!jcrNodeWrapper.hasProperty(name)) {
                return null;
            }
            JCRPropertyWrapper property = jcrNodeWrapper.getProperty(name);

            if (!property.isMultiple()) {
                return getString(property.getValue());
            } else {
                List<Object> res = new ArrayList<>();
                for (JCRValueWrapper value : property.getValues()) {
                    res.add(getString(value));
                }
                return res;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private Object getString(JCRValueWrapper value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return SpecializedTypesHandler.getNode(value.getNode());
            default:
                return value.getString();
        }
    }
}
