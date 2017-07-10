package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRNodeInput")
public class DXGraphQLJCRNodeInput {

    public DXGraphQLJCRNodeInput(@GraphQLName("name") String name,
                                 @GraphQLName("primaryNodeType") String primaryNodeType,
                                 @GraphQLName("properties") List<DXGraphQLJCRPropertyInput> properties,
                                 @GraphQLName("children") List<DXGraphQLJCRNodeInput> children) {
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
    public List<DXGraphQLJCRPropertyInput> properties;

    @GraphQLField
    public List<DXGraphQLJCRNodeInput> children;

}
