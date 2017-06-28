package org.jahia.modules.graphql.provider.dxm.model;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import graphql.schema.*;
import org.codehaus.plexus.util.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by toto on 15/06/17.
 */
public class DXGraphQLConnection<T> {
    protected static GraphQLOutputType pageInfoType;

    private Iterable<T> iterable;

    private List<DXGraphQLEdge<T>> edges;

    private String before;
    private String after;
    private Integer beforeOffset;
    private Integer afterOffset;
    private Integer first;
    private Integer last;

    private CursorFetcher<T> cursorFetcher;

    public DXGraphQLConnection(Iterable<T> nodes, DataFetchingEnvironment env, CursorFetcher<T> cursorFetcher) {
        this(nodes, env.<String>getArgument("before"), env.<String>getArgument("after"), env.<Integer>getArgument("beforeOffset"), env.<Integer>getArgument("afterOffset"), env.<Integer>getArgument("first"), env.<Integer>getArgument("last"), cursorFetcher);
    }

    public DXGraphQLConnection(Iterable<T> iterable, String before, String after, Integer beforeOffset, Integer afterOffset, Integer first, Integer last, CursorFetcher<T> cursorFetcher) {
        this.iterable = iterable;
        this.before = before;
        this.after = after;
        this.beforeOffset = beforeOffset;
        this.afterOffset = afterOffset;
        this.first = first;
        this.last = last;
        this.cursorFetcher = cursorFetcher;
    }

    public static Builder newConnectionFieldDefinition() {
        return new Builder();
    }

    public static class Builder extends GraphQLFieldDefinition.Builder {
        private String name;
        private CursorFetcher cursorFetcher;
        private GraphQLOutputType nodeType;

        @Override
        public Builder name(String name) {
            this.name = name;
            super.name(name);
            return this;
        }

        public Builder xx() {
            return this;
        }

        @Override
        public Builder type(GraphQLOutputType type) {
            this.nodeType = type;
            return this;
        }

        @Override
        public Builder dataFetcher(final DataFetcher dataFetcher) {
            super.dataFetcher(new DataFetcher() {
                @Override
                public Object get(DataFetchingEnvironment environment) {
                    Object o = dataFetcher.get(environment);
                    if (o instanceof Iterable) {
                        return new DXGraphQLConnection((Iterable<DXGraphQLNode>) o,environment, cursorFetcher);
                    } else if (o instanceof DXGraphQLConnection) {
                        return o;
                    }
                    throw new IllegalArgumentException("Invalid result from datafetcher, should be list or connection : " +o);
                }
            });
            if (dataFetcher instanceof CursorFetcher) {
                this.cursorFetcher = (CursorFetcher) dataFetcher;
            }
            return this;
        }

        @Override
        public GraphQLFieldDefinition build() {
            super.type(getConnectionType(StringUtils.capitalise(name), nodeType, cursorFetcher != null));
            super.argument(getConnectionFieldArguments(cursorFetcher != null));
            return super.build();
        }
    }

    public static GraphQLOutputType getConnectionType(String name, GraphQLOutputType outputType, boolean withCursor) {
        GraphQLObjectType.Builder edgeObjectTypeBuilder = newObject()
                .name(name + "Edge")
                .field(newFieldDefinition().name("node")
                        .type(outputType)
                        .description("The item at the end of the edge"))
                .field(newFieldDefinition().name("offset")
                        .type(GraphQLInt)
                        .description("index of this item into the connection"));
        if (withCursor) {
            edgeObjectTypeBuilder = edgeObjectTypeBuilder
                    .field(newFieldDefinition().name("cursor")
                            .type(GraphQLString)
                            .description("cursor marks a unique position or index into the connection"));
        }
        return newObject()
                .name(name + "Connection")
                .field(newFieldDefinition().name("pageInfo")
                        .type(new GraphQLNonNull(new GraphQLTypeReference("PageInfo")))
                        .description("details about this specific page"))
                .field(newFieldDefinition().name("nodes")
                        .type(new GraphQLList(outputType))
                        .description("direct access to the list of items at the end of the edges"))
                .field(newFieldDefinition().name("edges")
                        .type(new GraphQLList(edgeObjectTypeBuilder.build()))
                        .description("the list of edges of this connection"))
                .build();
    }

    public static GraphQLOutputType getPageInfoType() {
        if (pageInfoType == null) {
            pageInfoType = newObject().name("PageInfo")
                    .description("Information about pagination in a connection.")
                    .field(newFieldDefinition().name("startCursor")
                            .type(GraphQLString)
                            .description("When paginating backwards, the cursor to continue."))
                    .field(newFieldDefinition().name("endCursor")
                            .type(GraphQLString)
                            .description("When paginating forwards, the cursor to continue."))
                    .field(newFieldDefinition().name("hasPreviousPage")
                            .type(new GraphQLNonNull(GraphQLBoolean))
                            .description("When paginating backwards, are there more items?"))
                    .field(newFieldDefinition().name("hasNextPage")
                            .type(new GraphQLNonNull(GraphQLBoolean))
                            .description("When paginating forwards, are there more items?"))
                    .field(newFieldDefinition().name("startOffset")
                            .type(GraphQLInt)
                            .description("When paginating backwards, the offset to continue."))
                    .field(newFieldDefinition().name("endOffset")
                            .type(GraphQLInt)
                            .description("When paginating forwards, the offset to continue."))
                    .field(newFieldDefinition().name("count")
                            .type(GraphQLInt)
                            .description("The number of nodes in the current page."))
                    .field(newFieldDefinition().name("totalCount")
                            .type(GraphQLInt)
                            .description("The total number of nodes in this connection."))
                    .build();
        }
        return pageInfoType;
    }

    public static List<GraphQLArgument> getConnectionFieldArguments(boolean withCursor) {
        List<GraphQLArgument> args = new ArrayList<>();
        if (withCursor) {
            args.add(newArgument()
                    .name("before")
                    .description("fetching only nodes before this node (exclusive)")
                    .type(GraphQLString)
                    .build());
            args.add(newArgument()
                    .name("after")
                    .description("fetching only nodes after this node (exclusive)")
                    .type(GraphQLString)
                    .build());
        }
        args.add(newArgument()
                .name("beforeOffset")
                .description("fetching only the first certain number of nodes")
                .type(GraphQLInt)
                .build());
        args.add(newArgument()
                .name("afterOffset")
                .description("fetching only the first certain number of nodes")
                .type(GraphQLInt)
                .build());
        args.add(newArgument()
                .name("first")
                .description("fetching only the first certain number of nodes")
                .type(GraphQLInt)
                .build());
        args.add(newArgument()
                .name("last")
                .description("fetching only the last certain number of nodes")
                .type(GraphQLInt)
                .build());
        return args;
    }

    public Iterable<T> getIterable() {
        return iterable;
    }

    public List<DXGraphQLEdge<T>> getEdges() {
        if (edges == null) {
            edges = new ArrayList<>();
            boolean started = getAfter() == null && getAfterOffset() == null;
            Integer offset = 0;
            for (T node : getIterable()) {
                if (started) {
                    if ((getBeforeOffset() != null && offset.equals(getBeforeOffset())) || (getBefore() != null && cursorFetcher.getCursor(node).equals(getBefore()))) {
                        break;
                    }

                    edges.add(new DXGraphQLEdge<T>(node, cursorFetcher != null ? cursorFetcher.getCursor(node) : null, offset));

                    if (getFirst() != null && edges.size() == getFirst()) {
                        break;
                    }
                } else if ((getAfterOffset() != null && offset.equals(getAfterOffset())) || (getAfter() != null && cursorFetcher.getCursor(node).equals(getAfter()))) {
                    started = true;
                }
                offset++;
            }
            if (getLast() != null) {
                edges = edges.subList(edges.size() - getLast(), edges.size());
            }
        }
        return edges;
    }

    public List<T> getNodes() {
        return Lists.transform(getEdges(), new Function<DXGraphQLEdge<T>, T>() {
            @Override
            public T apply(@Nullable DXGraphQLEdge<T> tdxGraphQLEdge) {
                return tdxGraphQLEdge != null ? tdxGraphQLEdge.getNode() : null;
            }
        });
    }

    public DXGraphQLPageInfo getPageInfo() {
        return new DXGraphQLPageInfo(this);
    }


    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public Integer getBeforeOffset() {
        return beforeOffset;
    }

    public Integer getAfterOffset() {
        return afterOffset;
    }

    public Integer getFirst() {
        return first;
    }

    public Integer getLast() {
        return last;
    }

    public static interface CursorFetcher<T> {
        public String getCursor(T node);
    }
}
