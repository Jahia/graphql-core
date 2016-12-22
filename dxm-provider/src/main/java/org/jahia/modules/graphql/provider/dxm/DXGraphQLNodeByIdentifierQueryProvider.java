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
public class DXGraphQLNodeByIdentifierQueryProvider implements GraphQLQueryProvider {
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
                if (dataFetchingEnvironment.getArgument("ids") != null) {
                    List<String> ids = dataFetchingEnvironment.getArgument("ids");
                    for (String id : ids) {
                        try {
                            nodes.add(JCRSessionFactory.getInstance().getCurrentUserSession().getNodeByIdentifier(id));
                        } catch (RepositoryException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                String asMixin = dataFetchingEnvironment.getArgument("asMixin");

                List<DXGraphQLNode> qlnodes = new ArrayList<>();
                for (JCRNodeWrapper jcrNodeWrapper : nodes) {
                    qlnodes.add(new DXGraphQLNode(jcrNodeWrapper, asMixin));
                }
                return nodeBuilder.getList(qlnodes);
            }
        };
    }

    @Override
    public GraphQLObjectType getQuery() {
        return newObject().name("nodesById")
                .field(newFieldDefinition()
                        .name("nodesById")
                        .type(nodeBuilder.getListType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
                        .argument(newArgument()
                                .name("ids")
                                .description("List of IDs")
                                .type(new GraphQLList(GraphQLString))
                                .build())
                        .argument(newArgument()
                                .name("asMixin")
                                .description("Specify a mixin that will be used for the node")
                                .type(GraphQLString)
                                .build())
                        .dataFetcher(getNodesDataFetcher())
                        .build())
                .build();
    }

}
