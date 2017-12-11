package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

@GraphQLTypeExtension(GqlJcrNode.class)
public class JCRNodeExtensions {

    private GqlJcrNode node;

    public JCRNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    public  String testExtension(@GraphQLName("arg") String arg) {
        return "test " + node.getName() + " - " + arg;
    }

}
