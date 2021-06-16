package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLFieldCompleter;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.services.modulemanager.spi.Config;
import org.jahia.services.modulemanager.spi.ConfigService;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesValues;

import javax.inject.Inject;
import java.io.IOException;

public class GqlConfigurationMutation implements DXGraphQLFieldCompleter {

    private final String pid;
    private final String identifier;
    @Inject
    @GraphQLOsgiService
    private ConfigService configService;
    private Config configuration = null;

    public GqlConfigurationMutation(String pid, String identifier) {
        this.pid = pid;
        this.identifier = identifier;
    }

    @GraphQLField
    public Value mutateValues() {
        return new Value(getConfiguration().getValues());
    }


    private Config getConfiguration() {
        try {
            if (identifier == null) {
                configuration = configService.getConfig(pid);
            } else {
                configuration = configService.getConfig(pid, identifier);
            }
        } catch (IOException e) {
            throw new DataFetchingException(e);
        }
        return configuration;
    }

    @Override
    public void completeField() {
        if (configuration != null) {
            try {
                configService.storeConfig(configuration);
            } catch (IOException e) {
                throw new DataFetchingException(e);
            }
        }
    }

    @GraphQLName("ConfigurationItemValues")
    public class Value {
        PropertiesValues v;

        public Value(PropertiesValues v) {
            this.v = v;
        }

        @GraphQLField
        @GraphQLDescription("Modify a structured object")
        public Value mutateObjectValue(@GraphQLName("name") String name) {
            return new Value(v.getValues(name));
        }

        @GraphQLField
        @GraphQLDescription("Modify a list of items")
        public List mutateList(@GraphQLName("name") String name) {
            return new List(v.getList(name));
        }

        @GraphQLField
        @GraphQLDescription("Set a string property value")
        public String setStringValue(@GraphQLName("name") String name, @GraphQLName("value") String value) {
            v.setProperty(name, value);
            return value;
        }

        @GraphQLField
        @GraphQLDescription("Set a numeric property value")
        public int setNumberValue(@GraphQLName("name") String name, @GraphQLName("value") int value) {
            v.setIntegerProperty(name, value);
            return value;
        }

    }

    @GraphQLName("ConfigurationItemsList")
    public class List {
        PropertiesList l;

        public List(PropertiesList l) {
            this.l = l;
        }

        @GraphQLField
        @GraphQLDescription("Adds a new structured object to the list")
        public Value addObjectValue() {
            return new Value(l.addValues());
        }

        @GraphQLField
        @GraphQLDescription("Adds a string property value to the list")
        public String addStringValue(@GraphQLName("value") String value) {
            l.addProperty(value);
            return value;
        }

        @GraphQLField
        @GraphQLDescription("Adds a numeric property value to the list")
        public int addNumberValue(@GraphQLName("value") int value) {
            l.addIntegerProperty(value);
            return value;
        }
    }
}
