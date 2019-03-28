package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

public class NumberFinder extends Finder {
    private String numberType;

    private NumberFinder() {
    }

    public static NumberFinder fromFinder(Finder finder) {
        NumberFinder f = new NumberFinder();
        f.setType(finder.getType());
        f.setProperty(finder.getProperty());
        f.setName(finder.getName());
        return f;
    }

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }
}
