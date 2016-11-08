package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.*;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLProperty;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.render.RenderContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

@Component(service = DXGraphQLNodeBuilder.class)
public class DXGraphQLNodeBuilder extends DXGraphQLBuilder {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNodeBuilder.class);

    private DXGraphQLPropertiesBuilder propertiesBuilder;

    private DXGraphQLNodeTypeBuilder nodeTypeBuilder;

    @Override
    public String getName() {
        return "node";
    }

    @Reference(service = DXGraphQLExtender.class, target = "(graphQLType=node)")
    public void bindExtender(DXGraphQLExtender extender) {
        this.extenders.add(extender);
    }

    public void unbindExtender(DXGraphQLExtender extender) {
        this.extenders.remove(extender);
    }

    @Reference
    public void setNodeTypeBuilder(DXGraphQLNodeTypeBuilder nodeTypeBuilder) {
        this.nodeTypeBuilder = nodeTypeBuilder;
    }

    @Reference
    public void setPropertiesBuilder(DXGraphQLPropertiesBuilder propertiesBuilder) {
        this.propertiesBuilder = propertiesBuilder;
    }


    @Override
    public GraphQLObjectType.Builder build(GraphQLObjectType.Builder builder) {
        return builder
                .field(newFieldDefinition()
                        .name("identifier")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("path")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("parentPath")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("parentIdentifier")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("primaryNodeType")
                        .type(nodeTypeBuilder.getType())
                        .dataFetcher(getNodeTypeDataFetcher())
                        .build())
                .field(newFieldDefinition()
                        .name("mixinTypes")
                        .type(new GraphQLList(nodeTypeBuilder.getType()))
                        .build())
                .field(newFieldDefinition()
                        .name("properties")
                        .type(new GraphQLList(propertiesBuilder.getType()))
                        .argument(newArgument().name("names")
                                .type(new GraphQLList(GraphQLString))
                                .defaultValue(Collections.emptyList())
                                .build())
                        .dataFetcher(getPropertiesDataFetcher())
                        .build())
                .field(newFieldDefinition()
                        .name("children")
                        .type(new GraphQLTypeReference("nodeList"))
                        .dataFetcher(getChildrenDataFetcher())
                        .build());
    }

    public DataFetcher getNodeTypeDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DXGraphQLNode) {
                    try {
                        return new DXGraphQLNodeType(((DXGraphQLNode) environment.getSource()).getNode().getPrimaryNodeType());
                    } catch (RepositoryException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                return null;
            }
        };
    }

    public DataFetcher getChildrenDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                DXGraphQLNode node = (DXGraphQLNode) environment.getSource();
                List<DXGraphQLNode> children = new ArrayList<DXGraphQLNode>();
                try {
                    for (JCRNodeWrapper child : node.getNode().getNodes()) {
                        children.add(new DXGraphQLNode(child));
                    }

                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
                return getList(children);
            }
        };
    }

    public DataFetcher getPropertiesDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
                List<DXGraphQLProperty> propertyList = new ArrayList<DXGraphQLProperty>();
                try {
                    List<String> names = dataFetchingEnvironment.getArgument("names");
                    if (names != null && !names.isEmpty()) {
                        for (String name : names) {
                            if (node.getNode().hasProperty(name)) {
                                propertyList.add(new DXGraphQLProperty(node.getNode().getProperty(name)));
                            }
                        }
                    } else {
                        PropertyIterator pi = node.getNode().getProperties();
                        while (pi.hasNext()) {
                            JCRPropertyWrapper property = (JCRPropertyWrapper) pi.nextProperty();
                            propertyList.add(new DXGraphQLProperty(property));
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
                return propertyList;
            }
        };
    }

}
