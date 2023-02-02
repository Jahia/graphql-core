/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.jahia.modules.graphql.provider.dxm.osgiconfig.GqlConfigurationQuery;
import org.jahia.modules.graphql.provider.dxm.scheduler.GqlScheduler;
import org.jahia.utils.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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

    /**
     * Get getJahiaDatabase
     *
     * @return GqlJahiaDatabase
     * @throws SQLException
     */
    @GraphQLField
    @GraphQLDescription("Details about the database Jahia is connected to")
    public GqlJahiaDatabase getDatabase() throws SQLException {
        GqlJahiaDatabase gqlJahiaDatabase = new GqlJahiaDatabase();
        gqlJahiaDatabase.setType(DatabaseUtils.getDatabaseType().toString());
        Connection connection = null;
        try {
            connection = DatabaseUtils.getDatasource().getConnection();
            DatabaseMetaData metadata = connection.getMetaData();
        
            gqlJahiaDatabase.setName(metadata.getDatabaseProductName());
            gqlJahiaDatabase.setVersion(metadata.getDatabaseProductVersion());
            gqlJahiaDatabase.setDriverName(metadata.getDriverName());
            gqlJahiaDatabase.setDriverVersion(metadata.getDriverVersion());
            gqlJahiaDatabase.setUrl(metadata.getURL());
        } catch (Exception e) {
            logger.error("Unable to get database information . Cause: " + e.getMessage(), e);
        } finally {
            DatabaseUtils.closeQuietly(connection);
        }
        return gqlJahiaDatabase;
    }

    /**
     * We must have at least one field for the schema to be valid
     *
     * @return true
     */
    @GraphQLField
    @GraphQLDescription("Read an OSGi configuration")
    public GqlConfigurationQuery configuration(@GraphQLName("pid") @GraphQLDescription("Configuration pid ot factory pid") @GraphQLNonNull String pid,
                                               @GraphQLName("identifier") @GraphQLDescription("If factory pid, configuration identifier (filename suffix)") String identifier) {
        return new GqlConfigurationQuery(pid, identifier);
    }

    @GraphQLField
    @GraphQLDescription("Get jobs scheduler")
    public GqlScheduler getScheduler() {
        return new GqlScheduler();
    }
}
