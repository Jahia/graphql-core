package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

@GraphQLTypeExtension(GqlJcrNode.class)
public class JCRNodeExtensions {

    @GraphQLField
    public static String testExtension(DataFetchingEnvironment env, @GraphQLName("arg") String arg) {
        GqlJcrNode n = env.getSource();
        return "test " + n.getName() + " - " + arg;
    }

}
