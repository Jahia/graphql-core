package org.jahia.modules.graphql.provider;

import java.util.ArrayList;
import java.util.List;

/**
 * A GraphQL version of the JCR content node type
 */
public class DXGraphQLNode {

    private String identifier;
    private String path;
    private String parentIdentifier;
    private String parentPath;
    private String primaryNodeType;
    private List<String> mixinTypes = new ArrayList<>();
    private List<DXGraphQLProperty> properties = new ArrayList<>();

    public DXGraphQLNode(String identifier, String path, String parentIdentifier, String parentPath, String primaryNodeType, List<String> mixinTypes, List<DXGraphQLProperty> properties) {
        this.identifier = identifier;
        this.path = path;
        this.parentIdentifier = parentIdentifier;
        this.parentPath = parentPath;
        this.primaryNodeType = primaryNodeType;
        this.mixinTypes = mixinTypes;
        this.properties = properties;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPath() {
        return path;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getPrimaryNodeType() {
        return primaryNodeType;
    }

    public List<String> getMixinTypes() {
        return mixinTypes;
    }

    public List<DXGraphQLProperty> getProperties() {
        return properties;
    }
}
