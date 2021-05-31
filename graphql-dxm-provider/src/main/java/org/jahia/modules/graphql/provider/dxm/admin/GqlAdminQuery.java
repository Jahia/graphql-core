package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;


/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("adminQuery")
@GraphQLDescription("Admin queries root")
public class GqlAdminQuery {

    public static final Logger logger = LoggerFactory.getLogger(GqlAdminQuery.class);
    public static String JAHIA_PROJECT_VERSION;

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
    @GraphQLRequiresPermission(value = "admin")
    public GqlJahiaVersion getJahiaVersion()  {

        GqlJahiaVersion gqlJahiaVersion = new GqlJahiaVersion();
        gqlJahiaVersion.setRelease(Optional.ofNullable(JAHIA_PROJECT_VERSION).orElse(""));
        gqlJahiaVersion.setBuild(String.valueOf(Jahia.getBuildNumber()));
        gqlJahiaVersion.setSnapshot(Optional.ofNullable(JAHIA_PROJECT_VERSION).orElse("").contains("SNAPSHOT"));

        //Formatting buildDate to ISO8601
        Date date = null;
        try {
            date = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH).parse(Jahia.getBuildDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            gqlJahiaVersion.setBuildDate(ISO8601.format(calendar));
        } catch (ParseException e) {
            logger.warn("Exception while parsing build date",e);
        }
        return gqlJahiaVersion;
    }

    /**
     * Get Build Datetime
     *
     * @return String datetime in ISO8601 format
     */
    @GraphQLField
    @GraphQLName("datetime")
    @GraphQLDescription("Current datetime")
    public String getDatetime() {
        return ISO8601.format(Calendar.getInstance());
    }

    static {
        Properties p = new Properties();
        try {
            p.load(Constants.class.getClassLoader().getResourceAsStream("version.properties"));
        } catch (IOException e) {
            logger.warn("Exception while retrieving JAHIA_PROJECT_VERSION",e);
            throw new RuntimeException(e);
        }
        JAHIA_PROJECT_VERSION = p.getProperty("version");
    }
}
