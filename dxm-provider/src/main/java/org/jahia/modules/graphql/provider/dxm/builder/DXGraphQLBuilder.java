package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public abstract class DXGraphQLBuilder {

    protected GraphQLOutputType type;
    protected GraphQLOutputType edgeType;
    protected GraphQLOutputType listType;

    protected List<DXGraphQLExtender> extenders = new ArrayList<>();

    public abstract String getName();

    public GraphQLOutputType getType() {
        if (type == null) {
            GraphQLObjectType.Builder builder = newObject()
                    .name(getName());

            builder.fields(getFields());

            for (DXGraphQLExtender extender : extenders) {
                builder.fields(extender.getFields());
            }

            type = builder.build();
        }
        return type;
    }

    public GraphQLOutputType getEdgeType() {

        if (edgeType == null) {
            edgeType = newObject()
                    .name(getName()+"Edge")
                    .field(newFieldDefinition().name("node")
                            .type(getType())
                            .build())
                    .field(newFieldDefinition().name("cursor")
                            .type(GraphQLString)
                            .build())
                    .build();
        }
        return edgeType;
    }

    public GraphQLOutputType getListType() {
        if (listType == null) {
            listType = newObject()
                    .name(getName() + "List")
                    .field(newFieldDefinition().name("totalCount")
                            .type(GraphQLInt)
                            .build())
                    .field(newFieldDefinition().name(getName() + "s")
                            .type(new GraphQLList(getType()))
                            .build())
                    .field(newFieldDefinition().name("edges")
                            .type(new GraphQLList(getEdgeType()))
                            .build())
                    .build();
        }
        return listType;
    }

    public static Object getList(List nodes) {
        HashMap<String, Object> list = new HashMap<String, Object>();
        list.put("totalCount", nodes.size());
        list.put("nodes", nodes);
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> edge = new HashMap<String, Object>();
            edge.put("node",node);
            edge.put("cursor",Integer.toString(node.hashCode()));
            edges.add(edge);
        }
        list.put("edges", edges);
        return list;
    }


    protected abstract List<GraphQLFieldDefinition> getFields();

}
