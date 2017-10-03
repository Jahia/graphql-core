package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

@GraphQLName("JCRProperty")
public class GqlJcrProperty {
    private static Logger logger = LoggerFactory.getLogger(GqlJcrProperty.class);
    private JCRPropertyWrapper propertyWrapper;
    private GqlJcrNode node;

    public GqlJcrProperty(JCRPropertyWrapper propertyWrapper, GqlJcrNode node) {
        this.propertyWrapper = propertyWrapper;
        this.node = node;
    }

    @GraphQLNonNull
    @GraphQLField
    public String getName() {
        try {
            return propertyWrapper.getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public GqlJcrPropertyType getType() {
        try {
            return GqlJcrPropertyType.getValue(propertyWrapper.getType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
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
            throw new RuntimeException(e);
        }
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
            throw new RuntimeException(e);
        }
    }

    public GqlJcrNode getParentNode() {
        if (node == null) {
            try {
                node = SpecializedTypesHandler.getNode(propertyWrapper.getParent());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }

}
