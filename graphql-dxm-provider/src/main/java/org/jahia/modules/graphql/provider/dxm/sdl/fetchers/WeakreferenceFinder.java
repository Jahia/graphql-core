package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

public class WeakreferenceFinder extends Finder {

    private String referencedType;

    private WeakreferenceFinder() {
    }

    public static WeakreferenceFinder fromFinder(Finder finder) {
        WeakreferenceFinder f = new WeakreferenceFinder();
        f.setType(finder.getType());
        f.setProperty(finder.getProperty());
        f.setName(finder.getName());
        return f;
    }

    public String getReferencedType() {
        return referencedType;
    }

    public void setReferencedType(String referencedType) {
        this.referencedType = referencedType;
    }
}
