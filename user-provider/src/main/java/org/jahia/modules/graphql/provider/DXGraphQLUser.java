package org.jahia.modules.graphql.provider;

import java.util.Properties;

/**
 * Created by loom on 10.10.16.
 */
public class DXGraphQLUser {

    String id;
    Properties properties = new Properties();

    public DXGraphQLUser(String id, Properties properties) {
        this.id = id;
        if (properties != null) {
            this.properties = properties;
        }
    }

    public String getId() {
        return id;
    }

    public Properties getProperties() {
        return properties;
    }
}
