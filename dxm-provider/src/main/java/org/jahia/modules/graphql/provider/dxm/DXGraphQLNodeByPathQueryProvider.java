package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.servlet.GraphQLQueryProvider;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeBuilder;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Component(service = GraphQLQueryProvider.class, immediate = true)
public class DXGraphQLNodeByPathQueryProvider implements GraphQLQueryProvider {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNodeByPathQueryProvider.class);

    private DXGraphQLNodeBuilder nodeBuilder;

    @Reference
    public void setNodeBuilder(DXGraphQLNodeBuilder nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
    }

    @Override
    public Object context() {
        return "rootcontext";
    }

    public DataFetcher getNodesDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                List<JCRNodeWrapper> nodes = new ArrayList<>();
                if (dataFetchingEnvironment.getArgument("paths") != null) {
                    List<String> paths = dataFetchingEnvironment.getArgument("paths");
                    for (String path : paths) {
                        try {
                            nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession().getNode(path));
                        } catch (RepositoryException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                List<DXGraphQLNode> qlnodes = new ArrayList<>();
                for (JCRNodeWrapper jcrNodeWrapper : nodes) {
                    qlnodes.add(new DXGraphQLNode(jcrNodeWrapper));
                }
                return nodeBuilder.getList(qlnodes);
            }
        };
    }

    @Override
    public GraphQLObjectType getQuery() {
        return newObject().name("nodesByPath")
                .field(newFieldDefinition()
                        .name("nodesByPath")
                        .type(nodeBuilder.getListType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a paths argument.")
                        .argument(newArgument()
                                .name("paths")
                                .description("List of paths")
                                .type(new GraphQLList(GraphQLString))
                                .build())
                        .dataFetcher(getNodesDataFetcher())
                        .build())
                .build();
    }
}
