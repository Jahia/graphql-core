package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRNodeInput")
public class DXGraphQLJCRNodeInput {

    public DXGraphQLJCRNodeInput(HashMap map) {
        name = (String) map.get("name");
        primaryNodeType = (String) map.get("primaryNodeType");
        children = new ArrayList<>();
        List<HashMap> l = (List<HashMap>) map.get("children");
        if (l != null) {
            for (HashMap m : l) {
                children.add(new DXGraphQLJCRNodeInput(m));
            }
        }
    }

    @GraphQLField
    public String name;

    @GraphQLField
    public String primaryNodeType;

    @GraphQLField
    public List<DXGraphQLJCRNodeInput> children;

//    @GraphQLField
//    public List<DXGraphQLJCRPropertyInput> properties;

}
