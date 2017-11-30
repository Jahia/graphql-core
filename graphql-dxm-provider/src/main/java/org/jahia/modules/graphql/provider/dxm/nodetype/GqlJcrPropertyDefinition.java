package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * TODO Comment me
 *
 * @author toto
 */
@GraphQLName("PropertyDefinition")
public class GqlJcrPropertyDefinition {
    private String name;
    private int requiredType;

    @GraphQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLField
    public int getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(int requiredType) {
        this.requiredType = requiredType;
    }
}
