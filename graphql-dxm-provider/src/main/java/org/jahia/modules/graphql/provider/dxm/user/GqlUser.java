package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.services.usermanager.JahiaUser;

@GraphQLName("User")
@GraphQLDescription("GraphQL representation of a Jahia user")
public class GqlUser {
    private JahiaUser user;

    public GqlUser(JahiaUser jahiaUser) {
        this.user = jahiaUser;
    }

    @GraphQLField
    @GraphQLDescription("User name")
    public String getName() {
        return user.getName();
    }

    @GraphQLField
    @GraphQLDescription("User property")
    public String getProperty(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property") String name) {
        return user.getProperty(name);
    }
}
