package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRPropertyInput")
public class DXGraphQLJCRPropertyInput {

    public DXGraphQLJCRPropertyInput(HashMap map) {
        name = (String) map.get("name");
    }

    @GraphQLField
    public String name;

    @GraphQLField
    public String language;

    @GraphQLField
    public DXGraphQLPropertyType type;

    @GraphQLField
    public String value;

    @GraphQLField
    public List<String> values;

}
