package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultEdge;

public class DXEdge<T> extends DefaultEdge<T> {

    private int index;

    public DXEdge(T node, int index, ConnectionCursor cursor) {
        super(node, cursor);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
