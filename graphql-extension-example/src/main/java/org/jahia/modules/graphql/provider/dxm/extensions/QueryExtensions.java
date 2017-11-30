package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class QueryExtensions {

    @GraphQLField
    public static String testExtension(@GraphQLName("arg") String arg) {
        return "test " + arg;
    }

}
