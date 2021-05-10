package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;


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
