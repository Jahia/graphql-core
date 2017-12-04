package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.connection.PaginatedData;

public interface DXPaginatedData<T> extends PaginatedData<T> {

    int getTotalCount();

    int getNodesCount();

    int getIndex(T entity);

}
