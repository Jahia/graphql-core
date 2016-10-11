package org.jahia.modules.graphql.provider;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.servlet.GraphQLQueryProvider;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        GraphQLObjectType propertyType = DXGraphQLCommonTypeProvider.getPropertyType();

        DataFetcher propertiesFetcher = new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                DXGraphQLUser user = (DXGraphQLUser) environment.getSource();
                List<DXGraphQLProperty> propertyList = new ArrayList<DXGraphQLProperty>();
                for (String propertyName : user.getProperties().stringPropertyNames()) {
                    DXGraphQLProperty property = new DXGraphQLProperty(propertyName, user.getProperties().getProperty(propertyName));
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
                        .description("Unique identifier for the user inside the DX instance")
                        .build())
                .field(newFieldDefinition()
                        .type(new GraphQLList(propertyType))
                        .dataFetcher(propertiesFetcher)
                        .name("properties")
                        .description("User properties")
                        .build())
                .build();

        DataFetcher userDataFetcher = new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                List<DXGraphQLUser> users = new ArrayList<DXGraphQLUser>();
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
                        DXGraphQLUser user = new DXGraphQLUser(jcrUserNode.getUserKey(), properties);
                        users.add(user);
                    }
                }
                return users;
            }
        };

        /*
        GraphQLObjectType.Builder nodeTypesBuilder = newObject()
                .name("nodeTypes");

        NodeTypeRegistry.JahiaNodeTypeIterator jahiaNodeTypeIterator = nodeTypeRegistry.getAllNodeTypes();
        for (ExtendedNodeType extendedNodeType : jahiaNodeTypeIterator) {
            if ("*".equals(extendedNodeType.getName())) {
                continue;
            }
            GraphQLObjectType.Builder nodeTypeBuilder = newObject().name(extendedNodeType.getName().replaceAll(":", "_"));
            ExtendedPropertyDefinition[] extendedPropertyDefinitions = extendedNodeType.getDeclaredPropertyDefinitions();
            for (ExtendedPropertyDefinition extendedPropertyDefinition : extendedPropertyDefinitions) {
                if ("*".equals(extendedPropertyDefinition.getName())) {
                    continue;
                }
                nodeTypeBuilder.field(newFieldDefinition()
                        .name(extendedPropertyDefinition.getName().replaceAll(":", "_"))
                        .type(DXGraphQLCommonTypeProvider.getGraphQLType(extendedPropertyDefinition.getRequiredType(), extendedPropertyDefinition.isMultiple()))
                        .build());
            }
            GraphQLObjectType nodeType = nodeTypeBuilder.build();
            nodeTypesBuilder.field(newFieldDefinition()
                    .name(nodeType.getName())
                    .type(nodeType)
                    .build()
            );
        }
        */

        GraphQLObjectType queryType = newObject()
                .name("dx")
                .description("GraphQL Root object to access DX objects and content")
                .field(newFieldDefinition()
                        .type(new GraphQLList(userType))
                        .name("user")
                        .description("Provides access to users inside of DX, notably by requesting them through a userKey argument.")
                        .argument(newArgument()
                                .name("userKey")
                                .description("A unique string identifier for the user inside the DX instance.")
                                .type(GraphQLString)
                                .build())
                        .dataFetcher(userDataFetcher)
                        .build())
                /*
                .field(newFieldDefinition()
                        .type(nodeTypesBuilder.build())
                        .name("nodeTypes")
                        .build()
                )
                */
                .build();

        return queryType;
    }

    @Override
    public Object context() {
        return new DXGraphQLUser("root", null);
    }

}
