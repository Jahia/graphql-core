package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRNodeInput")
public class GqlJcrNodeInput {

    public GqlJcrNodeInput(@GraphQLName("name") String name,
                                 @GraphQLName("primaryNodeType") String primaryNodeType,
                                 @GraphQLName("properties") List<GqlJcrPropertyInput> properties,
                                 @GraphQLName("children") List<GqlJcrNodeInput> children) {
        this.name = name;
        this.primaryNodeType = primaryNodeType;
        this.properties = properties;
        this.children = children;
    }

    @GraphQLField
    public String name;

    @GraphQLField
    public String primaryNodeType;

    @GraphQLField
    public List<GqlJcrPropertyInput> properties;

    @GraphQLField
    public List<GqlJcrNodeInput> children;

}
