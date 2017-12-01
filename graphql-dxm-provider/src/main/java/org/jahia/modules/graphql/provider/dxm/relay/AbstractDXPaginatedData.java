package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.connection.AbstractPaginatedData;

public abstract class AbstractDXPaginatedData<T> extends AbstractPaginatedData<T> implements DXPaginatedData<T> {
    private int nodesCount;
    private int totalCount;

    public AbstractDXPaginatedData(Iterable<T> data, boolean hasPreviousPage, boolean hasNextPage, int nodesCount, int totalCount) {
        super(hasPreviousPage, hasNextPage, data);
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
