package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;


/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("JahiaAdminQuery")
@GraphQLDescription("Jahia admin queries root")
public class GqlJahiaAdminQuery {

    public static final Logger logger = LoggerFactory.getLogger(GqlJahiaAdminQuery.class);
    public static final String JAHIA_PROJECT_VERSION;

    static {
        Properties p = new Properties();
        try {
            p.load(Constants.class.getClassLoader().getResourceAsStream("version.properties"));
        } catch (IOException e) {
            logger.warn("Exception while retrieving JAHIA_PROJECT_VERSION", e);
            throw new RuntimeException(e);
        }
        JAHIA_PROJECT_VERSION = p.getProperty("version");
    }

    /**
     * Get getJahiaVersion
     *
     * @return GqlJahiaVersion
     */
    @GraphQLField
    @GraphQLDescription("Version of the running Jahia instance")
    public GqlJahiaVersion getVersion() {
        GqlJahiaVersion gqlJahiaVersion = new GqlJahiaVersion();
        gqlJahiaVersion.setRelease(Optional.ofNullable(JAHIA_PROJECT_VERSION).orElse(""));
        try {
            gqlJahiaVersion.setBuild(MethodUtils.invokeExactStaticMethod(Jahia.class, "getBuildNumber", new Object[0]).toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.warn("Cannot get build number");
        }
        gqlJahiaVersion.setSnapshot(Optional.ofNullable(JAHIA_PROJECT_VERSION).orElse("").contains("SNAPSHOT"));

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
