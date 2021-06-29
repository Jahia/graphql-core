package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.bin.Jahia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("JahiaAdminQuery")
@GraphQLDescription("Jahia admin queries root")
public class GqlJahiaAdminQuery {

    public static final Logger logger = LoggerFactory.getLogger(GqlJahiaAdminQuery.class);

    /**
     * Get getJahiaVersion
     *
     * @return GqlJahiaVersion
     */
    @GraphQLField
    @GraphQLDescription("Version of the running Jahia instance")
    public GqlJahiaVersion getVersion() {

        GqlJahiaVersion gqlJahiaVersion = new GqlJahiaVersion();
        gqlJahiaVersion.setRelease(Jahia.getFullProductVersion());
        try {
            gqlJahiaVersion.setBuild(MethodUtils.invokeExactStaticMethod(Jahia.class, "getBuildNumber", new Object[0]).toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.warn("Cannot get build number");
        }
        //Formatting buildDate to ISO8601
        Date date = null;
        try {
            date = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH).parse(Jahia.getBuildDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            gqlJahiaVersion.setBuildDate(ISO8601.format(calendar));
        } catch (ParseException e) {
            logger.warn("Exception while parsing build date", e);
        }
        return gqlJahiaVersion;
    }
}
