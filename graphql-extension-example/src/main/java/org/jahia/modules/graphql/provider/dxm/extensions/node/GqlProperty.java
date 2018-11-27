package org.jahia.modules.graphql.provider.dxm.extensions.node;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Created at Nov 2018$
 *
 * @author chooliyip
 **/

public class GqlProperty {

    private String name;

    private String type;

    private String prefix;

    private String[] defaultValues;

    @GraphQLField
    @GraphQLName("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @GraphQLField
    @GraphQLName("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @GraphQLField
    @GraphQLName("defaultValues")
    public String[] getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(String[] defaultValues) {
        this.defaultValues = defaultValues;
    }

    @GraphQLField
    @GraphQLName("prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
