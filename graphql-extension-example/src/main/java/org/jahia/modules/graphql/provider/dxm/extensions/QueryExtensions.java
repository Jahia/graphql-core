package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLTypeExtension;
import graphql.servlet.GraphQLQuery;

@GraphQLTypeExtension(GraphQLQuery.class)
public class QueryExtensions {

    @GraphQLField
    public static String testExtension(@GraphQLName("arg") String arg) {
        return "test " + arg;
    }

}
