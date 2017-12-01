package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.relay.Relay;
import graphql.schema.*;

import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class DXRelay extends Relay {

    private final GraphQLObjectType pageInfoType = newObject()
            .name("PageInfo")
            .description("Information about pagination in a connection.")
            .field(newFieldDefinition()
                    .name("hasNextPage")
                    .type(new GraphQLNonNull(GraphQLBoolean))
                    .description("When paginating forwards, are there more items?"))
            .field(newFieldDefinition()
                    .name("hasPreviousPage")
                    .type(new GraphQLNonNull(GraphQLBoolean))
                    .description("When paginating backwards, are there more items?"))
            .field(newFieldDefinition()
                    .name("startCursor")
                    .type(GraphQLString)
                    .description("When paginating backwards, the cursor to continue."))
            .field(newFieldDefinition()
                    .name("endCursor")
                    .type(GraphQLString)
                    .description("When paginating forwards, the cursor to continue."))
            .field(newFieldDefinition()
                    .name("nodesCount")
                    .type(GraphQLInt)
                    .description("When paginating forwards, the cursor to continue."))
            .field(newFieldDefinition()
                    .name("totalCount")
                    .type(GraphQLInt)
                    .description("When paginating forwards, the cursor to continue."))
            .build();


    @Override
    public GraphQLObjectType connectionType(String name, GraphQLObjectType edgeType, List<GraphQLFieldDefinition> connectionFields) {
        return newObject()
                .name(name + "Connection")
                .description("A connection to a list of items.")
                .field(newFieldDefinition()
                        .name("nodes")
                        .description("a list of nodes")
                        .type(new GraphQLList(edgeType.getFieldDefinition("node").getType())))
                .field(newFieldDefinition()
                        .name("edges")
                        .description("a list of edges")
                        .type(new GraphQLList(edgeType)))
                .field(newFieldDefinition()
                        .name("pageInfo")
                        .description("details about this specific page")
                        .type(new GraphQLNonNull(pageInfoType)))
                .fields(connectionFields)
                .build();
    }


    public List<GraphQLArgument> getConnectionFieldArguments() {
        List<GraphQLArgument> args = super.getConnectionFieldArguments();
        args.add(newArgument()
                .name("offset")
                .description("fetching only nodes after this node (inclusive)")
                .type(GraphQLInt)
                .build());
        args.add(newArgument()
                .name("limit")
                .description("fetching only the first certain number of nodes")
                .type(GraphQLInt)
                .build());
        return args;
    }

    @Override
    public GraphQLObjectType edgeType(String name, GraphQLOutputType nodeType, GraphQLInterfaceType nodeInterface, List<GraphQLFieldDefinition> edgeFields) {
        return newObject()
                .name(name + "Edge")
                .description("An edge in a connection")
                .field(newFieldDefinition()
                        .name("node")
                        .type(nodeType)
                        .description("The item at the end of the edge"))
                .field(newFieldDefinition()
                        .name("cursor")
                        .type(new GraphQLNonNull(GraphQLString))
                        .description("cursor marks a unique position or index into the connection"))
                .field(newFieldDefinition()
                        .name("index")
                        .type(GraphQLInt)
                        .description("index in the connection"))
                .fields(edgeFields)
                .build();
    }
}
