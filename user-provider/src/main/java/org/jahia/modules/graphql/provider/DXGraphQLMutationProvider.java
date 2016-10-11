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
                .argument(newArgument()
                        .name("name")
                        .type(GraphQLString).build())
                .argument(newArgument()
                        .name("parentPath")
                        .type(GraphQLString).build())
                .type(DXGraphQLCommonTypeProvider.getDXNodeType())
                .dataFetcher(new DataFetcher() {
                    @Override
                    public Object get(DataFetchingEnvironment environment) {
                        String name = environment.getArgument("name");
                        String parentPath = environment.getArgument("parentPath");
                        logger.debug("Mutation createNodeByPath called with arguments name=" + name + " parentPath=" + parentPath);
                        return new DXGraphQLNode(UUID.randomUUID().toString(), name, UUID.randomUUID().toString(), parentPath, "primaryNodeType", new ArrayList<String>(), new ArrayList<DXGraphQLProperty>());
                    }
                })
                .build();

        mutations.add(newNodeMutation);
        return mutations;
    }
}
