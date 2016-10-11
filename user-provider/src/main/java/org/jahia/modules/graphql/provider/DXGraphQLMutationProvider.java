package org.jahia.modules.graphql.provider;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.GraphQLMutationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * A GraphQL Mutation provider for DX.
 */
public class DXGraphQLMutationProvider implements GraphQLMutationProvider {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLMutationProvider.class);

    @Override
    public Collection<GraphQLFieldDefinition> getMutations() {
        List<GraphQLFieldDefinition> mutations = new ArrayList<>();

        GraphQLFieldDefinition newNodeMutation = newFieldDefinition()
                .name("createNodeByPath")
                .description("Create a new content node in DX's content repository")
                .argument(newArgument()
                        .name("name")
                        .description("A name for the new child node, avoid non ASCII characters or spaces")
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name("parentPath")
                        .description("A valid path for the parent node under which this child node will be created.")
                        .type(GraphQLString)
                        .build()
                )
                .argument(newArgument()
                        .name("nodeTypeName")
                        .description("A valid node type name for the child node that will be created under the parent node.")
                        .type(GraphQLString)
                        .build()
                )
                .type(DXGraphQLCommonTypeProvider.getDXNodeType())
                .dataFetcher(new DataFetcher() {
                    @Override
                    public Object get(DataFetchingEnvironment environment) {
                        String name = environment.getArgument("name");
                        String parentPath = environment.getArgument("parentPath");
                        String nodeTypeName = environment.getArgument("nodeTypeName");
                        logger.debug("Mutation createNodeByPath called with arguments name=" + name + " parentPath=" + parentPath);
                        return new DXGraphQLNode(UUID.randomUUID().toString(), name, UUID.randomUUID().toString(), parentPath, nodeTypeName, new ArrayList<String>(), new ArrayList<DXGraphQLProperty>());
                    }
                })
                .build();

        mutations.add(newNodeMutation);
        return mutations;
    }
}
