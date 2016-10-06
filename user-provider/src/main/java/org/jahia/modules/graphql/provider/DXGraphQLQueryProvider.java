package org.jahia.modules.graphql.provider;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import graphql.servlet.*;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A GraphQL query provider for DX
 */
@Component(service=GraphQLQueryProvider.class)
public class DXGraphQLQueryProvider implements GraphQLQueryProvider {

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
                return new User("1", null);
            }
        };

        GraphQLObjectType queryType = newObject()
                .name("users")
                .field(newFieldDefinition()
                        .type(userType)
                        .name("user")
                        .dataFetcher(userDataFetcher)
                        .build())
                .build();
        return queryType;
    }

    @Override
    public Object context() {
        return new User("root", null);
    }
}
