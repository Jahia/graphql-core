package org.jahia.modules.graphql.provider.dxm.osgiconfig;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;

@GraphQLDescription("OSGi configuration property")
public class GqlConfigurationProperty {
    private String key;
    private String value;

    public GqlConfigurationProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @GraphQLField
    @GraphQLDescription("The property key")
    public String getKey() {
        return key;
    }

    @GraphQLField
    @GraphQLDescription("The property value")
    public String getValue() {
        return value;
    }
}
