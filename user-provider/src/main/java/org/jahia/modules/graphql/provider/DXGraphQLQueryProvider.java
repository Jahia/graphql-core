package org.jahia.modules.graphql.provider;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.servlet.GraphQLQueryProvider;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUserManagerService;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * A GraphQL query provider for DX
 */
public class DXGraphQLQueryProvider implements GraphQLQueryProvider {

    private JahiaUserManagerService jahiaUserManagerService;

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Override
    public GraphQLObjectType getQuery() {

        GraphQLObjectType propertyType = newObject()
                .name("property")
                .field(newFieldDefinition()
                        .name("key")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("value")
                        .type(GraphQLString)
                        .build())
                .build();

        DataFetcher propertiesFetcher = new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                User user = (User) environment.getSource();
                List<Property> propertyList = new ArrayList<Property>();
                for (String propertyName : user.getProperties().stringPropertyNames()) {
                    Property property = new Property(propertyName, user.getProperties().getProperty(propertyName));
                    propertyList.add(property);
                }
                return propertyList;
            }
        };

        GraphQLObjectType userType = newObject()
                .name("user")
                .description("Represents a single user")
                .field(newFieldDefinition()
                        .type(GraphQLString)
                        .name("id")
                        .build())
                .field(newFieldDefinition()
                        .type(new GraphQLList(propertyType))
                        .dataFetcher(propertiesFetcher)
                        .name("properties")
                        .build())
                .build();

        DataFetcher userDataFetcher = new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                List<User> users = new ArrayList<User>();
                String userKey = dataFetchingEnvironment.getArgument("userKey");
                if (userKey != null) {
                    JCRUserNode jcrUserNode = jahiaUserManagerService.lookup(userKey);
                    if (jcrUserNode != null) {
                        Properties properties = new Properties();
                        try {
                            for (Map.Entry<String, String> propertyEntry : jcrUserNode.getPropertiesAsString().entrySet()) {
                                properties.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
                            }
                        } catch (RepositoryException e) {
                            e.printStackTrace();
                        }
                        User user = new User(jcrUserNode.getUserKey(), properties);
                        users.add(user);
                    }
                }
                return users;
            }
        };

        GraphQLObjectType queryType = newObject()
                .name("users")
                .field(newFieldDefinition()
                        .type(new GraphQLList(userType))
                        .name("user")
                        .argument(newArgument().name("userKey").type(GraphQLString).build())
                        .dataFetcher(userDataFetcher)
                        .build())
                .build();
        return queryType;
    }

    @Override
    public Object context() {
        return new User("root", null);
    }

    public class User {

        String id;
        Properties properties = new Properties();

        public User(String id, Properties properties) {
            this.id = id;
            if (properties != null) {
                this.properties = properties;
            }
        }

        public String getId() {
            return id;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public class Property {
        String key;
        String value;

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
