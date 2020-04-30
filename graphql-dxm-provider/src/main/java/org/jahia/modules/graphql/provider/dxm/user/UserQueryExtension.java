package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRSessionFactory;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("A query extension that gives access to the users")
public class UserQueryExtension {

    @GraphQLField
    @GraphQLDescription("Get the current user")
    public static GqlUser getCurrentUser() {
        return new GqlUser(JCRSessionFactory.getInstance().getCurrentUser());
    }
}
