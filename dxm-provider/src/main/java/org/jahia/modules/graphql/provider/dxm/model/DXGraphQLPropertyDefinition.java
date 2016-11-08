package org.jahia.modules.graphql.provider.dxm.model;

/**
 * TODO Comment me
 *
 * @author toto
 */
public class DXGraphQLPropertyDefinition {
    private String name;
    private int requiredType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(int requiredType) {
        this.requiredType = requiredType;
    }
}
