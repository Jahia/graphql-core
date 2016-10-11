package org.jahia.modules.graphql.provider;

/**
 * Created by loom on 10.10.16.
 */
public class DXGraphQLProperty {
    String key;
    String value;

    public DXGraphQLProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
