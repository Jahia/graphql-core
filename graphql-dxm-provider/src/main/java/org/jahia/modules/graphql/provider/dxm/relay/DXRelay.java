/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
