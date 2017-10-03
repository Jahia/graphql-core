package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

@GraphQLName("JCRProperty")
public class GqlJcrProperty {

    private JCRPropertyWrapper propertyWrapper;

    public GqlJcrProperty(JCRPropertyWrapper propertyWrapper) {
        this.propertyWrapper = propertyWrapper;
    }

    @GraphQLField
    @GraphQLNonNull
    public String getName() {
        try {
            return propertyWrapper.getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public String getValue() {
        try {
            if (propertyWrapper.isMultiple()) {
                return null;
            }
            return propertyWrapper.getValue().getString();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    public List<String> getValues() {
        try {
            if (!propertyWrapper.isMultiple()) {
                return null;
            }
            List<String> values = new ArrayList<>();
            for (JCRValueWrapper wrapper : propertyWrapper.getValues()) {
                values.add(wrapper.getString());
            }
            return values;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
