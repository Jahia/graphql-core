package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.bin.Jahia;
import org.jahia.api.Constants;

/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("adminQuery")
@GraphQLDescription("Admin queries root")
public class GqlAdminQuery {

    /**
     * @deprecated replaced by jahia node
     */
    @Deprecated
    @GraphQLField
    @GraphQLName("version")
    @GraphQLNonNull
    @GraphQLDescription("Version of the running Jahia instance")
    public String getProductVersion() {
        return Jahia.getFullProductVersion();
    }

    /**
     * Get getJahiaVersion
     *
     * @return GqlJahiaVersion
     */
    @GraphQLField
    @GraphQLName("jahia")
    @GraphQLDescription("Version of the running Jahia instance")
    public GqlJahiaVersion getJahiaVersion() {
        return new GqlJahiaVersion(
                Constants.JAHIA_PROJECT_VERSION,
                String.valueOf(Jahia.getBuildNumber()),
                Constants.JAHIA_PROJECT_VERSION.contains("SNAPSHOT")
        );
    }

    /**
     * Get Build Datetime
     *
     * @return String datetime
     */
    @GraphQLField
    @GraphQLName("datetime")
    @GraphQLDescription("Build Datetime of the running Jahia instance")
    public String getDatetime() {
        return Jahia.getBuildDate();
    }

}
