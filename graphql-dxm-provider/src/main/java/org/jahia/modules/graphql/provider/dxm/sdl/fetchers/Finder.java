package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

public class Finder {
    protected String name;
    protected String property;
    protected String type;
    protected boolean multiple;

    public Finder() {
    }

    public Finder(String name) {
        this.name = name;

        if (name.equals("all")) {
            multiple = true;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
