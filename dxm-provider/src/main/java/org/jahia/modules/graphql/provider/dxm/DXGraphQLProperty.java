package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

@GraphQLName("JCRProperty")
public class DXGraphQLProperty {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLProperty.class);
    private JCRPropertyWrapper propertyWrapper;

    public DXGraphQLProperty(JCRPropertyWrapper propertyWrapper) {
        this.propertyWrapper = propertyWrapper;
    }

    @GraphQLField
    public String getKey() {
        try {
            return propertyWrapper.getName();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @GraphQLField
    public String getValue() {
        try {
            if (propertyWrapper.isMultiple()) {
                return "[multiple]";
            } else {
                return propertyWrapper.getValue().getString();
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @GraphQLField
    public List<String> getValues() {
        try {
            List<String> values = new ArrayList<>();
            if (propertyWrapper.isMultiple()) {
                for (JCRValueWrapper wrapper : propertyWrapper.getValues()) {
                    values.add(wrapper.getString());
                }
            } else {
                values.add(propertyWrapper.getValue().getString());
            }
            return values;
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
