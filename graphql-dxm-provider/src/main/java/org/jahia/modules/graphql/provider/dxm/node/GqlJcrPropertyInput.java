package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRPropertyInput")
public class GqlJcrPropertyInput {

    public GqlJcrPropertyInput(@GraphQLName("name") String name,
                                     @GraphQLName("language") String language,
                                     @GraphQLName("type") GqlJcrPropertyType type,
                                     @GraphQLName("value") String value,
                                     @GraphQLName("values") List<String> values) {
        this.name = name;
        this.language = language;
        this.type = type;
        this.value = value;
        this.values = values;
    }

    @GraphQLField
    public String name;

    @GraphQLField
    public String language;

    @GraphQLField
    public GqlJcrPropertyType type;

    @GraphQLField
    public String value;

    @GraphQLField
    public List<String> values;

}
