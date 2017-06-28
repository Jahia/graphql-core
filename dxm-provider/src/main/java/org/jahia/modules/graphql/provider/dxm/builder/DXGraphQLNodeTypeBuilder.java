package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeDefinition;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLPropertyDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.jahia.modules.graphql.provider.dxm.model.DXGraphQLConnection.newConnectionFieldDefinition;

@Component(service = DXGraphQLNodeTypeBuilder.class)
public class DXGraphQLNodeTypeBuilder extends DXGraphQLBuilder<DXGraphQLNodeType> {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNodeTypeBuilder.class);
    private NodeTypeRegistry nodeTypeRegistry;
    private List<GraphQLFieldDefinition> fieldDefinitionList;

    @Reference(service = NodeTypeRegistry.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setNodeTypeRegistry(NodeTypeRegistry nodeTypeRegistry) {
        this.nodeTypeRegistry = nodeTypeRegistry;
    }

    @Override
    public String getName() {
        return "NodeType";
    }

    @Override
    protected List<GraphQLFieldDefinition> getFields() {
        if (fieldDefinitionList == null) {
            fieldDefinitionList = Arrays.asList(
                    newFieldDefinition()
                            .name("name")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("abstract")
                            .type(GraphQLBoolean)
                            .build(),
                    newFieldDefinition()
                            .name("mixin")
                            .type(GraphQLBoolean)
                            .build(),
                    newFieldDefinition()
                            .name("displayName")
                            .type(GraphQLString)
                            .argument(newArgument().name("language")
                                    .type(GraphQLString)
                                    .build())
                            .dataFetcher(new DisplayNameDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("properties")
                            .type(new GraphQLList(newObject().name("PropertyDefinition")
                                    .field(newFieldDefinition()
                                            .name("name")
                                            .type(GraphQLString)
                                            .build())
                                    .build()))
                            .dataFetcher(new PropertyDefinitionsDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("nodes")
                            .type(new GraphQLList(newObject().name("NodeDefinition")
                                    .field(newFieldDefinition()
                                            .name("name")
                                            .type(GraphQLString)
                                            .build())
                                    .build()))
                            .dataFetcher(new NodeDefinitionsDataFetcher())
                            .build(),
                    newConnectionFieldDefinition()
                            .name("subTypes")
                            .type(new GraphQLTypeReference("NodeType"))
                            .dataFetcher(new SubTypesDataFetcher())
                            .build(),
                    newConnectionFieldDefinition()
                            .name("superTypes")
                            .type(new GraphQLTypeReference("NodeType"))
                            .dataFetcher(new SuperTypesDataFetcher())
                            .build());
        }
        return fieldDefinitionList;
    }

    private class DisplayNameDataFetcher implements  DataFetcher {
        @Override
        public Object get(DataFetchingEnvironment environment) {
            DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
            String lang = environment.getArgument("language");
            return nodeType.getNodeType().getLabel(LanguageCodeConverters.languageCodeToLocale(lang));
        }
    }

    private class PropertyDefinitionsDataFetcher implements DataFetcher {
        @Override
        public Object get(DataFetchingEnvironment environment) {
            DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
            List<DXGraphQLPropertyDefinition> propertyList = null;
            try {
                ExtendedNodeType ent = nodeTypeRegistry.getNodeType(nodeType.getName());
                propertyList = new ArrayList<>();
                for (ExtendedPropertyDefinition definition : ent.getPropertyDefinitions()) {
                    DXGraphQLPropertyDefinition qlPropertyDefinition = new DXGraphQLPropertyDefinition();
                    qlPropertyDefinition.setName(definition.getName());
                    propertyList.add(qlPropertyDefinition);
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            }
            return propertyList;
        }
    }

    private class NodeDefinitionsDataFetcher implements DataFetcher {
        @Override
        public Object get(DataFetchingEnvironment environment) {
            DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
            List<DXGraphQLNodeDefinition> nodeList = null;
            try {
                ExtendedNodeType ent = nodeTypeRegistry.getNodeType(nodeType.getName());
                nodeList = new ArrayList<>();
                for (ExtendedNodeDefinition definition : ent.getChildNodeDefinitions()) {
                    DXGraphQLNodeDefinition qlNodeDefinition = new DXGraphQLNodeDefinition();
                    qlNodeDefinition.setName(definition.getName());
                    nodeList.add(qlNodeDefinition);
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            }
            return nodeList;
        }
    }

    private class SubTypesDataFetcher implements DataFetcher {
        @Override
        public Object get(DataFetchingEnvironment environment) {
            DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
            List<DXGraphQLNodeType> subTypes = null;
            try {
                ExtendedNodeType ent = nodeTypeRegistry.getNodeType(nodeType.getName());
                subTypes = new ArrayList<>();
                for (ExtendedNodeType type : ent.getSubtypesAsList()) {
                    subTypes.add(new DXGraphQLNodeType(type));
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            }
            return subTypes;
        }
    }

    private class SuperTypesDataFetcher implements DataFetcher {
        @Override
        public Object get(DataFetchingEnvironment environment) {
            DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
            List<DXGraphQLNodeType> superTypes = null;
            try {
                ExtendedNodeType ent = nodeTypeRegistry.getNodeType(nodeType.getName());
                superTypes = new ArrayList<>();
                for (ExtendedNodeType type : ent.getSupertypeSet()) {
                    superTypes.add(new DXGraphQLNodeType(type));
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            }
            return superTypes;
        }
    }
}
