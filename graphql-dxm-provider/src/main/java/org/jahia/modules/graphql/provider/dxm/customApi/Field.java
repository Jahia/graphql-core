package org.jahia.modules.graphql.provider.dxm.customApi;

public class Field {
    private String name;
    private String property;

    public Field(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
