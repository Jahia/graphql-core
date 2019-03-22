package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import java.util.Map;

public class WeakreferenceFinder extends Finder {

    private String referencedType;
    private Map<String, String> referenceTypeProps;
    private String referencedTypeSDLName;

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

    public Map<String, String> getReferenceTypeProps() {
        return referenceTypeProps;
    }

    public void setReferenceTypeProps(Map<String, String> referenceTypeProps) {
        this.referenceTypeProps = referenceTypeProps;
    }

    public String getReferencedTypeSDLName() {
        return referencedTypeSDLName;
    }

    public void setReferencedTypeSDLName(String referencedTypeSDLName) {
        this.referencedTypeSDLName = referencedTypeSDLName;
    }
}
