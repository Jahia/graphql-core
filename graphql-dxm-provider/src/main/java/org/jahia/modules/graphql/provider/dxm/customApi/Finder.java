package org.jahia.modules.graphql.provider.dxm.customApi;

import graphql.schema.DataFetcher;

public class Finder {
    private String name;
    private String property;
    private String type;
    private boolean multiple;

    public Finder(String name) {
        this.name = name;

        if (name.equals("all")) {
            multiple = true;
        }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

}
