package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("adminQuery")
@GraphQLDescription("Admin queries root")
public class GqlAdminQuery {

    public static final Logger logger = LoggerFactory.getLogger(GqlAdminQuery.class);

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
    public GqlJahiaVersion getJahiaVersion()  {

        GqlJahiaVersion gqlJahiaVersion = new GqlJahiaVersion();
        gqlJahiaVersion.setRelease(Constants.JAHIA_PROJECT_VERSION);
        gqlJahiaVersion.setBuild(String.valueOf(Jahia.getBuildNumber()));
        gqlJahiaVersion.setSnapshot(Constants.JAHIA_PROJECT_VERSION.contains("SNAPSHOT"));

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
     * @return String datetime
     */
    @GraphQLField
    @GraphQLName("datetime")
    @GraphQLDescription("Current datetime")
    public String getDatetime() {
        return ISO8601.format(Calendar.getInstance());
    }

}
