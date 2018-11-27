package org.jahia.modules.graphql.provider.dxm.extensions.node;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Created at Nov 2018$
 *
 * @author chooliyip
 **/
public class GqlName {

    private String name;

    public GqlName(String name){
        this.name = name;
    }

    @GraphQLField
    @GraphQLName("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
