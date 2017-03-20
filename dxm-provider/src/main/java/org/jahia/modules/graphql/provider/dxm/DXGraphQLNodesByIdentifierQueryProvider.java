package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNonNull;
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
public class DXGraphQLNodesByIdentifierQueryProvider implements GraphQLQueryProvider {
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

    public DataFetcher getNodeDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
                List<JCRNodeWrapper> nodes = new ArrayList<>();
                String asMixin = dataFetchingEnvironment.getArgument("asMixin");
                String id = dataFetchingEnvironment.getArgument("id");
                try {
                    return new DXGraphQLNode(JCRSessionFactory.getInstance().getCurrentUserSession().getNodeByIdentifier(id), asMixin);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public GraphQLObjectType getQuery() {
        return newObject().name("nodeById")
                .field(newFieldDefinition()
                        .name("nodeById")
                        .type(nodeBuilder.getType())
                        .description("Provides access to nodes inside of DX, notably by requesting them through a ID argument.")
                        .argument(newArgument()
                                .name("id")
                                .description("List of IDs")
                                .type(new GraphQLNonNull(GraphQLString))
                                .build())
                        .argument(newArgument()
                                .name("asMixin")
                                .description("Specify a mixin that will be used for the node")
                                .type(GraphQLString)
                                .build())
                        .dataFetcher(getNodeDataFetcher())
                        .build())
                .build();
    }

}
