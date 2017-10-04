package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphQL representation of a JCR property.
 */
@GraphQLName("JCRProperty")
public class GqlJcrProperty {

    private JCRPropertyWrapper propertyWrapper;

    /**
     * Create an instance that represents a JCR property to GraphQL.
     *
     * @param propertyWrapper The JCR property to represent
     */
    public GqlJcrProperty(JCRPropertyWrapper propertyWrapper) {
        this.propertyWrapper = propertyWrapper;
    }

    /**
     * @return The name of the JCR property
     */
    @GraphQLField
    @GraphQLNonNull
    public String getName() {
        try {
            return propertyWrapper.getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The value of the JCR property as a String in case the property is single-valued, null otherwise
     */
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

    /**
     * @return The values of the JCR property as a Strings in case the property is multiple-valued, null otherwise
     */
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
