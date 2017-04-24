package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.*;
import graphql.servlet.GraphQLQueryProvider;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeBuilder;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.query.QueryWrapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

@Component(service = GraphQLQueryProvider.class)
public class DXGraphQLQueryProvider implements GraphQLQueryProvider {
    public static final String ID_ARG = "id";
    public static final String PATH_ARG = "path";
    public static final String IDS_ARG = "ids";
    public static final String PATHS_ARG = "paths";
    public static final String QUERY_ARG = "query";
    public static final String QUERY_LANGUAGE_ARG = "queryLanguage";
    private static Logger logger = LoggerFactory.getLogger(GraphQLQueryProvider.class);

    List<GraphQLFieldDefinition> queries = new ArrayList<>();

    private DXGraphQLNodeBuilder nodeBuilder;


    @Reference
    public void setNodeBuilder(DXGraphQLNodeBuilder nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
    }

    public DataFetcher getNodesDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                String asMixin = dataFetchingEnvironment.getArgument("asMixin");
                String workspace = dataFetchingEnvironment.getArgument("workspace");
                if (dataFetchingEnvironment.getArgument(ID_ARG) != null) {
                    String id = dataFetchingEnvironment.getArgument(ID_ARG);
                    try {
                        return new DXGraphQLNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(id), asMixin);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                } else if (dataFetchingEnvironment.getArgument(PATH_ARG) != null) {
                    String path = dataFetchingEnvironment.getArgument(PATH_ARG);
                    try {
                        return new DXGraphQLNode(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path), asMixin);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    List<JCRNodeWrapper> nodes = new ArrayList<>();

                    if (dataFetchingEnvironment.getArgument(IDS_ARG) != null) {
                        List<String> ids = dataFetchingEnvironment.getArgument(IDS_ARG);
                        for (String id : ids) {
                            try {
                                nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNodeByIdentifier(id));
                            } catch (RepositoryException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    } else if (dataFetchingEnvironment.getArgument(PATHS_ARG) != null) {
                        List<String> paths = dataFetchingEnvironment.getArgument(PATHS_ARG);
                        for (String path : paths) {
                            try {
                                nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getNode(path));
                            } catch (RepositoryException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    } else if (dataFetchingEnvironment.getArgument(QUERY_ARG) != null) {
                        String query = dataFetchingEnvironment.getArgument(QUERY_ARG);
                        try {
                            String argument = dataFetchingEnvironment.<String>getArgument(QUERY_LANGUAGE_ARG);
                            QueryWrapper q = JCRSessionFactory.getInstance().getCurrentUserSession(workspace).getWorkspace().getQueryManager().createQuery(query, argument.equals("SQL2") ? Query.JCR_SQL2 : Query.XPATH);
                            JCRNodeIteratorWrapper ni = q.execute().getNodes();
                            while (ni.hasNext()) {
                                JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
                                nodes.add(next);
                            }
                        } catch (RepositoryException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    List<DXGraphQLNode> qlnodes = new ArrayList<>();
                    for (JCRNodeWrapper jcrNodeWrapper : nodes) {
                        qlnodes.add(new DXGraphQLNode(jcrNodeWrapper, asMixin));
                    }
                    return DXGraphQLNodeBuilder.getList(qlnodes, "Node");
                }
            }
        };
    }


    @Activate
    public void activate() {
        GraphQLArgument asMixinArg = newArgument()
                .name("asMixin")
                .description("Specify a mixin that will be used for the node")
                .type(GraphQLString)
                .build();
        GraphQLArgument workspaceArg = newArgument()
                .name("workspace")
                .description("The workspace where to find the nodes")
                .type(GraphQLString)
                .build();

        queries = Arrays.asList(
                newFieldDefinition()
                        .name("nodeById")
                        .type(nodeBuilder.getType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
                        .argument(newArgument()
                                .name(ID_ARG)
                                .description("List of IDs")
                                .type(new GraphQLNonNull(GraphQLString))
                                .build())
                        .argument(asMixinArg)
                        .argument(workspaceArg)
                        .dataFetcher(getNodesDataFetcher())
                        .build(),
                newFieldDefinition()
                        .name("nodeByPath")
                        .type(nodeBuilder.getType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a paths argument.")
                        .argument(newArgument()
                                .name(PATH_ARG)
                                .description("Paths")
                                .type(new GraphQLNonNull(GraphQLString))
                                .build())
                        .argument(asMixinArg)
                        .argument(workspaceArg)
                        .dataFetcher(getNodesDataFetcher())
                        .build(),
                newFieldDefinition()
                        .name("nodesById")
                        .type(nodeBuilder.getListType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
                        .argument(newArgument()
                                .name(IDS_ARG)
                                .description("List of IDs")
                                .type(new GraphQLNonNull(new GraphQLList(GraphQLString)))
                                .build())
                        .argument(asMixinArg)
                        .argument(workspaceArg)
                        .dataFetcher(getNodesDataFetcher())
                        .build(),
                newFieldDefinition()
                        .name("nodesByPath")
                        .type(nodeBuilder.getListType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a paths argument.")
                        .argument(newArgument()
                                .name(PATHS_ARG)
                                .description("List of paths")
                                .type(new GraphQLNonNull(new GraphQLList(GraphQLString)))
                                .build())
                        .argument(asMixinArg)
                        .argument(workspaceArg)
                        .dataFetcher(getNodesDataFetcher())
                        .build(),
                newFieldDefinition()
                        .name("nodesByQuery")
                        .type(nodeBuilder.getListType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a query argument.")
                        .argument(newArgument()
                                .name(QUERY_ARG)
                                .description("The JCR query to execute")
                                .type(new GraphQLNonNull(GraphQLString))
                                .build())
                        .argument(newArgument()
                                .name(QUERY_LANGUAGE_ARG)
                                .description("JCR-SQL2 or XPath")
                                .type(GraphQLEnumType.newEnum().name("QueryType")
                                        .value("SQL2")
                                        .value("XPATH")
                                        .build())
                                .defaultValue("SQL2")
                                .build())
                        .argument(asMixinArg)
                        .argument(workspaceArg)
                        .dataFetcher(getNodesDataFetcher())
                        .build()
        );

    }

    @Override
    public Collection<GraphQLFieldDefinition> getQueries() {
        return queries;
    }
}
