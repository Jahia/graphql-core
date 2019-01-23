package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

public class Field {

    private String name;
    private String property;
    private String type;

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

    public String getType() {
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

}
