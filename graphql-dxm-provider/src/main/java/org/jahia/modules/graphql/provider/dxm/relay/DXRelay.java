/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.relay.Relay;
import graphql.schema.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class DXRelay extends Relay {

    private Map<String, GraphQLObjectType> connectionTypes = new HashMap<>();

    public void addConnectionType(String connectionType, GraphQLObjectType connectionOutputType) {
        connectionTypes.put(connectionType, connectionOutputType);
    }

    public Map<String, GraphQLObjectType> getConnectionTypes() {
        return connectionTypes;
    }

    public final GraphQLObjectType pageInfoType = newObject()
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
        GraphQLObjectType.Builder builder = newObject()
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
                .fields(connectionFields);

        if (connectionTypes.containsKey(name + "Connection")) {
            GraphQLObjectType graphQLOutputType = connectionTypes.get(name + "Connection");
            builder.fields(graphQLOutputType.getFieldDefinitions());
        }
        return builder.build();
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
