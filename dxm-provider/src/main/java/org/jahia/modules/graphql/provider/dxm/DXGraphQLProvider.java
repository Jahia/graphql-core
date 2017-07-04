package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLAnnotations;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import graphql.servlet.*;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = GraphQLProvider.class, immediate = true)
public class DXGraphQLProvider implements GraphQLQueryProvider, GraphQLMutationProvider, GraphQLTypesProvider, GraphQLAnnotatedClassProvider {
    private static Logger logger = LoggerFactory.getLogger(GraphQLQueryProvider.class);
//
//    public static final GraphQLArgument GraphQlIdArgument = newArgument()
//            .name("id")
//            .description("List of IDs")
//            .type(new GraphQLNonNull(GraphQLString))
//            .build();
//    public static final GraphQLArgument GraphQlPathArgument = newArgument()
//            .name("path")
//            .description("Paths")
//            .type(new GraphQLNonNull(GraphQLString))
//            .build();
//    public static final GraphQLArgument GraphQLNodeTypeNameArgument = newArgument()
//            .name("name")
//            .description("Node type name")
//            .type(new GraphQLNonNull(GraphQLString))
//            .build();
//    public static final GraphQLArgument GraphQlIdsArgument = newArgument()
//            .name("ids")
//            .description("List of IDs")
//            .type(new GraphQLNonNull(new GraphQLList(GraphQLString)))
//            .build();
//    public static final GraphQLArgument GrphQLPathsArgument = newArgument()
//            .name("paths")
//            .description("List of paths")
//            .type(new GraphQLNonNull(new GraphQLList(GraphQLString)))
//            .build();
//    public static final GraphQLArgument QraphQLQueryArgument = newArgument()
//            .name("query")
//            .description("The JCR query to execute")
//            .type(new GraphQLNonNull(GraphQLString))
//            .build();
//    public static final GraphQLArgument GraphQLQueryLanguageArgument = newArgument()
//            .name("queryLanguage")
//            .description("JCR-SQL2 or XPath")
//            .type(GraphQLEnumType.newEnum().name("QueryType")
//                    .value("SQL2")
//                    .value("XPATH")
//                    .build())
//            .defaultValue("SQL2")
//            .build();
//    public static final GraphQLArgument GraphQLWorkspaceArgument = newArgument()
//            .name("workspace")
//            .description("The workspace where to find the nodes")
//            .type(GraphQLString)
//            .build();
//    public static final GraphQLArgument GraphQLAsMixinArgument = newArgument()
//            .name("asMixin")
//            .description("Specify a mixin that will be used for the node")
//            .type(GraphQLString)
//            .build();
//
//    private List<GraphQLFieldDefinition> queries = null;
//    private List<GraphQLFieldDefinition> mutations = null;
//
//    private DXGraphQLNodeBuilder nodeBuilder;
//
//
//    @Reference
//    public void setFieldsResolver(FieldsResolver fieldsResolver) {
//        logger.info("Field resolver bound");
//    }
//
//    @Reference
//    public void setNodeBuilder(DXGraphQLNodeBuilder nodeBuilder) {
//        this.nodeBuilder = nodeBuilder;
//    }
//
//    public DataFetcher getNodesDataFetcher() {
//        return new DataFetcher() {
//            @Override
//            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
//                logger.info("loading : " + dataFetchingEnvironment.getArguments());
//                String asMixin = dataFetchingEnvironment.getArgument("asMixin");
//                String workspace = dataFetchingEnvironment.getArgument("workspace");
//                if (dataFetchingEnvironment.getArgument(GraphQlIdArgument.getName()) != null) {
//                    String id = dataFetchingEnvironment.getArgument(GraphQlIdArgument.getName());
//                    try {
//                        return new DXGraphQLJCRNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(id), asMixin);
//                    } catch (RepositoryException e) {
//                        throw new RuntimeException(e);
//                    }
//                } else if (dataFetchingEnvironment.getArgument(GraphQlPathArgument.getName()) != null) {
//                    String path = dataFetchingEnvironment.getArgument(GraphQlPathArgument.getName());
//                    try {
//                        return new DXGraphQLJCRNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path), asMixin);
//                    } catch (RepositoryException e) {
//                        throw new RuntimeException(e);
//                    }
//                } else {
//                    List<JCRNodeWrapper> nodes = new ArrayList<>();
//
//                    if (dataFetchingEnvironment.getArgument(GraphQlIdsArgument.getName()) != null) {
//                        List<String> ids = dataFetchingEnvironment.getArgument(GraphQlIdsArgument.getName());
//                        for (String id : ids) {
//                            try {
//                                nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(id));
//                            } catch (RepositoryException e) {
//                                logger.error(e.getMessage(), e);
//                            }
//                        }
//                    } else if (dataFetchingEnvironment.getArgument(GrphQLPathsArgument.getName()) != null) {
//                        List<String> paths = dataFetchingEnvironment.getArgument(GrphQLPathsArgument.getName());
//                        for (String path : paths) {
//                            try {
//                                nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path));
//                            } catch (RepositoryException e) {
//                                logger.error(e.getMessage(), e);
//                            }
//                        }
//                    } else if (dataFetchingEnvironment.getArgument(QraphQLQueryArgument.getName()) != null) {
//                        String query = dataFetchingEnvironment.getArgument(QraphQLQueryArgument.getName());
//                        try {
//                            String argument = dataFetchingEnvironment.<String>getArgument(GraphQLQueryLanguageArgument.getName());
//                            QueryWrapper q = JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getWorkspace().getQueryManager().createQuery(query, argument.equals("SQL2") ? Query.JCR_SQL2 : Query.XPATH);
//                            JCRNodeIteratorWrapper ni = q.execute().getNodes();
//                            while (ni.hasNext()) {
//                                JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
//                                nodes.add(next);
//                            }
//                        } catch (RepositoryException e) {
//                            logger.error(e.getMessage(), e);
//                        }
//                    }
//
//                    List<DXGraphQLJCRNode> qlnodes = new ArrayList<>();
//                    for (JCRNodeWrapper jcrNodeWrapper : nodes) {
//                        qlnodes.add(new DXGraphQLJCRNode(jcrNodeWrapper, asMixin));
//                    }
//                    return qlnodes;
//                }
//            }
//        };
//    }
//
//    public DataFetcher getNodeTypeDataFetcher() {
//        return new DataFetcher() {
//            @Override
//            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
//                logger.info("loading : " + dataFetchingEnvironment.getArguments());
//                String name = dataFetchingEnvironment.getArgument(GraphQLNodeTypeNameArgument.getName());
//                try {
//                    return new DXGraphQLNodeType(NodeTypeRegistry.getInstance().getNodeType(name));
//                } catch (NoSuchNodeTypeException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        };
//    }
//
//    @Override
//    public Collection<GraphQLFieldDefinition> getQueries() {
//        if (queries == null) {
//
//            DataFetcher nodesDataFetcher = getNodesDataFetcher();
//
//            DataFetcher nodeTypeDataFetcher = getNodeTypeDataFetcher();
//
//            queries = Arrays.asList(
//                    newFieldDefinition()
//                            .name("nodeById")
//                            .type(nodeBuilder.getType())
//                            .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
//                            .argument(GraphQlIdArgument)
//                            .argument(GraphQLAsMixinArgument)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(nodesDataFetcher)
//                            .build(),
//                    newFieldDefinition()
//                            .name("nodeByPath")
//                            .type(nodeBuilder.getType())
//                            .description("Provides access to nodes inside of DX, notably by requesting them through a paths argument.")
//                            .argument(GraphQlPathArgument)
//                            .argument(GraphQLAsMixinArgument)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(nodesDataFetcher)
//                            .build(),
//                    newConnectionFieldDefinition()
//                            .name("nodesById")
//                            .type(new GraphQLTypeReference("Node"))
//                            .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
//                            .argument(GraphQlIdsArgument)
//                            .argument(GraphQLAsMixinArgument)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(nodesDataFetcher)
//                            .build(),
//                    newConnectionFieldDefinition()
//                            .name("nodesByPath")
//                            .type(new GraphQLTypeReference("Node"))
//                            .description("Provides access to nodes inside of DX, notably by requesting them through a paths argument.")
//                            .argument(GrphQLPathsArgument)
//                            .argument(GraphQLAsMixinArgument)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(nodesDataFetcher)
//                            .build(),
//                    newConnectionFieldDefinition()
//                            .name("nodesByQuery")
//                            .type(new GraphQLTypeReference("Node"))
//                            .description("Provides access to nodes inside of DX, notably by requesting them through a query argument.")
//                            .argument(QraphQLQueryArgument)
//                            .argument(GraphQLQueryLanguageArgument)
//                            .argument(GraphQLAsMixinArgument)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(nodesDataFetcher)
//                            .build(),
//                    newFieldDefinition()
//                            .name("nodeTypeByName")
//                            .type(nodeTypeBuilder.getType())
//                            .description("Provides access to node type.")
//                            .argument(GraphQLNodeTypeNameArgument)
//                            .dataFetcher(nodeTypeDataFetcher)
//                            .build()
//            );
//        }
//        return queries;
//    }
//
//    @Override
//    public Collection<GraphQLFieldDefinition> getMutations() {
//
//        if (mutations == null) {
//            mutations = Arrays.asList(
//                    newFieldDefinition()
//                            .name("jcrSessionSave")
//                            .type(GraphQLBoolean)
//                            .argument(GraphQLWorkspaceArgument)
//                            .dataFetcher(new DataFetcher() {
//                                @Override
//                                public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
//                                    String workspace = dataFetchingEnvironment.getArgument(GraphQLWorkspaceArgument.getName());
//                                    try {
//                                        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
//                                        if (session.hasPendingChanges()) {
//                                            session.save();
//                                            return true;
//                                        }
//                                    } catch (RepositoryException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                    return false;
//                                }
//                            })
//                    .build()
//            );
//        }
//        return mutations;
//    }

    @Override
    public Collection<GraphQLFieldDefinition> getMutations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        return Collections.emptyList();
    }

    @Override
    public Collection<GraphQLType> getTypes() {
        List<GraphQLType> types = new ArrayList<>();
//        types.add(DXGraphQLConnection.getPageInfoType());
//        types.add(nodeBuilder.getGenericType());
//        types.addAll(nodeBuilder.getKnownTypes().values());
        types.add(GraphQLAnnotations.getInstance().getObject(DXGraphQLGenericJCRNode.class));
        types.addAll(JCRNodeTypeResolver.getKnownTypes().values());
//        types.add(nodeBuilder.getConnectionType());
//        types.add(nodeTypeBuilder.getConnectionType());
        return types;
    }

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(BaseQueries.class);
    }
}
