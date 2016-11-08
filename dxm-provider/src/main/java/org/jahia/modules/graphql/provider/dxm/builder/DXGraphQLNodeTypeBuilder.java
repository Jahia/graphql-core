package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeDefinition;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLPropertyDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;

import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Component(service = DXGraphQLNodeTypeBuilder.class)
public class DXGraphQLNodeTypeBuilder extends DXGraphQLBuilder {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNodeTypeBuilder.class);

    @Override
    public String getName() {
        return "nodeType";
    }

    @Reference(target = "(graphQLType=nodeType)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC)
    public void bindExtender(DXGraphQLExtender extender) {
        this.extenders.add(extender);
    }

    public void unbindExtender(DXGraphQLExtender extender) {
        this.extenders.remove(extender);
    }

    @Override
    public GraphQLObjectType.Builder build(GraphQLObjectType.Builder builder) {
        return builder
                .field(newFieldDefinition()
                        .name("name")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("abstract")
                        .type(GraphQLBoolean)
                        .build())
                .field(newFieldDefinition()
                        .name("mixin")
                        .type(GraphQLBoolean)
                        .build())
                .field(newFieldDefinition()
                        .name("properties")
                        .type(new GraphQLList(newObject().name("propertyDefinition")
                                .field(newFieldDefinition()
                                        .name("name")
                                        .type(GraphQLString)
                                        .build())
                                .build()))
                        .dataFetcher(getPropertyDefinitionsDataFetcher())
                        .build())
                .field(newFieldDefinition()
                        .name("nodes")
                        .type(new GraphQLList(newObject().name("nodeDefinition")
                                .field(newFieldDefinition()
                                        .name("name")
                                        .type(GraphQLString)
                                        .build())
                                .build()))
                        .dataFetcher(getNodeDefinitionsDataFetcher())
                        .build())
                .field(newFieldDefinition()
                        .name("subTypes")
                        .type(new GraphQLList(new GraphQLTypeReference("nodeType")))
                        .dataFetcher(getSubTypesDataFetcher())
                        .build());
    }

    public DataFetcher getPropertyDefinitionsDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
                List<DXGraphQLPropertyDefinition> propertyList = null;
                try {
                    ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
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
        };
    }

    public DataFetcher getNodeDefinitionsDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
                List<DXGraphQLNodeDefinition> nodeList = null;
                try {
                    ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
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
        };
    }

    public DataFetcher getSubTypesDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                DXGraphQLNodeType nodeType = (DXGraphQLNodeType) environment.getSource();
                List<DXGraphQLNodeType> subTypes = null;
                try {
                    ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
                    subTypes = new ArrayList<>();
                    for (ExtendedNodeType type : ent.getSubtypesAsList()) {
                        subTypes.add(new DXGraphQLNodeType(type));
                    }
                } catch (NoSuchNodeTypeException e) {
                    logger.error(e.getMessage(), e);
                }
                return subTypes;
            }
        };
    }

}
