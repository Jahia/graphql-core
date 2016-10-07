package org.jahia.modules.graphql.provider;

import graphql.schema.*;
import graphql.servlet.GraphQLQueryProvider;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * A GraphQL query provider for DX
 */
public class DXGraphQLQueryProvider implements GraphQLQueryProvider {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLQueryProvider.class);

    private JahiaUserManagerService jahiaUserManagerService;
    private NodeTypeRegistry nodeTypeRegistry;

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setNodeTypeRegistry(NodeTypeRegistry nodeTypeRegistry) {
        this.nodeTypeRegistry = nodeTypeRegistry;
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
                            logger.error("Error accessing user node " + jcrUserNode.getPath() + " properties", e);
                        }
                        User user = new User(jcrUserNode.getUserKey(), properties);
                        users.add(user);
                    }
                }
                return users;
            }
        };

        GraphQLObjectType.Builder nodeTypesBuilder = newObject()
                .name("nodes");

        NodeTypeRegistry.JahiaNodeTypeIterator jahiaNodeTypeIterator = nodeTypeRegistry.getAllNodeTypes();
        for (ExtendedNodeType extendedNodeType : jahiaNodeTypeIterator) {
            GraphQLObjectType.Builder nodeTypeBuilder = newObject().name(extendedNodeType.getName());
            ExtendedPropertyDefinition[] extendedPropertyDefinitions = extendedNodeType.getDeclaredPropertyDefinitions();
            for (ExtendedPropertyDefinition extendedPropertyDefinition : extendedPropertyDefinitions) {
                nodeTypeBuilder.field(newFieldDefinition()
                        .name(extendedPropertyDefinition.getName())
                        .type(getGraphQLType(extendedPropertyDefinition.getRequiredType(), extendedPropertyDefinition.isMultiple()))
                        .build());
            }
            GraphQLObjectType nodeType = nodeTypeBuilder.build();
            nodeTypesBuilder.field(newFieldDefinition()
                    .name(nodeType.getName())
                    .type(nodeType)
                    .build()
            );
        }

        GraphQLObjectType queryType = newObject()
                .name("dx")
                .field(newFieldDefinition()
                        .type(new GraphQLList(userType))
                        .name("user")
                        .argument(newArgument().name("userKey").type(GraphQLString).build())
                        .dataFetcher(userDataFetcher)
                        .build())
                .field(newFieldDefinition()
                        .type(nodeTypesBuilder.build())
                        .name("nodes")
                        .build()
                )
                .build();

        return queryType;
    }

    @Override
    public Object context() {
        return new User("root", null);
    }

    private GraphQLOutputType getGraphQLType(int jcrPropertyType, boolean multiValued) {
        if (multiValued) {
            switch (jcrPropertyType) {
                case PropertyType.BOOLEAN:
                    return new GraphQLList(GraphQLBoolean);
                case PropertyType.DATE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                    return new GraphQLList(GraphQLLong);
                case PropertyType.DOUBLE:
                    return new GraphQLList(GraphQLFloat);
                case PropertyType.BINARY:
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                case PropertyType.URI:
                case PropertyType.WEAKREFERENCE:
                    return new GraphQLList(GraphQLString);
            }
        } else {
            switch (jcrPropertyType) {
                case PropertyType.BOOLEAN:
                    return GraphQLBoolean;
                case PropertyType.DATE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                    return GraphQLLong;
                case PropertyType.DOUBLE:
                    return GraphQLFloat;
                case PropertyType.BINARY:
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                case PropertyType.URI:
                case PropertyType.WEAKREFERENCE:
                    return GraphQLString;
            }
        }
        logger.warn("Couldn't find equivalent GraphQL type for property type=" + jcrPropertyType + " will use string type instead !");
        return GraphQLString;
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
