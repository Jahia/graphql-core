package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.relay.DefaultConnection;
import graphql.relay.Edge;
import graphql.relay.PageInfo;

import java.util.List;
import java.util.stream.Collectors;

public class DXConnection<T> extends DefaultConnection<T> {
    public DXConnection(List<Edge<T>> edges, PageInfo pageInfo) {
        super(edges, pageInfo);
    }

    public List<T> getNodes() {
        return getEdges().stream().map(Edge::getNode).collect(Collectors.toList());
    }
}
