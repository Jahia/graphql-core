package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultPageInfo;

public class DXPageInfo extends DefaultPageInfo {
    private int nodesCount;
    private int totalCount;

    public DXPageInfo(ConnectionCursor startCursor, ConnectionCursor endCursor, boolean hasPreviousPage, boolean hasNextPage, int nodesCount, int totalCount) {
        super(startCursor, endCursor, hasPreviousPage, hasNextPage);
        this.nodesCount = nodesCount;
        this.totalCount = totalCount;
    }

    public int getNodesCount() {
        return nodesCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
