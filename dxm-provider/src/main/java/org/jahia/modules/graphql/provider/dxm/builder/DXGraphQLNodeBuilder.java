package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.*;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.functors.AllPredicate;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNodeType;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLProperty;
import org.jahia.services.content.JCRItemWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;

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
        GraphQLInputObjectType propertyFilterType = GraphQLInputObjectType.newInputObject().name("propertyFilter")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("key").type(GraphQLString).build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("value").type(GraphQLString).build())
                .build();

        return builder
                .field(newFieldDefinition()
                        .name("identifier")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("name")
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
                        .argument(newArgument().name("names")
                                .type(new GraphQLList(GraphQLString))
                                .defaultValue(Collections.emptyList())
                                .build())
                        .argument(newArgument().name("anyType")
                                .type(new GraphQLList(GraphQLString))
                                .defaultValue(Collections.emptyList())
                                .build())
                        .argument(newArgument().name("properties")
                                .type(new GraphQLList(propertyFilterType))
                                .defaultValue(Collections.emptyList())
                                .build())
                        .dataFetcher(getChildrenDataFetcher())
                        .build())
                .field(newFieldDefinition()
                        .name("ancestors")
                        .type(new GraphQLTypeReference("nodeList"))
                        .argument(newArgument().name("upToPath")
                                .type(GraphQLString)
                                .defaultValue("")
                                .build())
                        .dataFetcher(getAncestorsDataFetcher())
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
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
                List<DXGraphQLNode> children = new ArrayList<DXGraphQLNode>();
                try {
                    Iterator<JCRNodeWrapper> nodes = IteratorUtils.filteredIterator(node.getNode().getNodes().iterator(), getNodesPredicate(dataFetchingEnvironment));
                    while (nodes.hasNext()) {
                        children.add(new DXGraphQLNode(nodes.next()));
                    }
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
                return getList(children);
            }
        };
    }

    public DataFetcher getAncestorsDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
                List<DXGraphQLNode> ancestors = new ArrayList<DXGraphQLNode>();

                String upToPath = dataFetchingEnvironment.getArgument("upToPath");
                String upToPathSlash = upToPath + "/";

                try {
                    List<JCRItemWrapper> jcrAncestors = node.getNode().getAncestors();
                    for (JCRItemWrapper ancestor : jcrAncestors) {
                        if (upToPath == null || ancestor.getPath().equals(upToPath) || ancestor.getPath().startsWith(upToPathSlash)) {
                            ancestors.add(new DXGraphQLNode((JCRNodeWrapper) ancestor));
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
                return getList(ancestors);
            }
        };
    }

    private AllPredicate<JCRNodeWrapper> getNodesPredicate(DataFetchingEnvironment dataFetchingEnvironment) {
        final List<String> names = dataFetchingEnvironment.getArgument("names");
        final List<String> anyType = dataFetchingEnvironment.getArgument("anyType");
        final List<Map> properties = dataFetchingEnvironment.getArgument("properties");

        return new AllPredicate<JCRNodeWrapper>(
                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {
                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        return names == null || names.isEmpty() || names.contains(node.getName());
                    }
                },
                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {
                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        if (anyType == null || anyType.isEmpty()) {
                            return true;
                        }
                        for (String type : anyType) {
                            try {
                                if (node.isNodeType(type)) {
                                    return true;
                                }
                            } catch (RepositoryException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        return false;
                    }
                },
                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {
                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        if (properties == null || properties.isEmpty()) {
                            return true;
                        }
                        for (Map property : properties) {
                            String key = (String) property.get("key");
                            String value = (String) property.get("value");
                            try {
                                if (!node.hasProperty(key) || !node.getProperty(key).getString().equals(value)) {
                                    return false;
                                }
                            } catch (RepositoryException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        return true;
                    }
                }
        );
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
