package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.bin.Jahia;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

import java.util.Calendar;


/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("AdminQuery")
@GraphQLDescription("Admin queries root")
public class GqlAdminQuery {

    /**
     * Get Jahia admin query
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get Jahia admin query")
    @GraphQLRequiresPermission(value = "admin")
    public GqlJahiaAdminQuery getJahia() {
        return new GqlJahiaAdminQuery();
    }

    /**
     * @deprecated replaced by jahia node
     */
    @Deprecated
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Version of the running Jahia instance")
    public String getVersion() {
        return Jahia.getFullProductVersion();
    }

    /**
     * Get Build Datetime
     *
     * @return String datetime in ISO8601 format
     */
    @GraphQLField
    @GraphQLDescription("Current datetime")
    public String getDatetime() {
        return ISO8601.format(Calendar.getInstance());
    }
}
