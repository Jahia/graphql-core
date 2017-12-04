package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.connection.ConnectionFetcher;
import graphql.relay.*;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DXPaginatedDataConnectionFetcher<T> implements ConnectionFetcher<T> {

    private DataFetcher<DXPaginatedData<T>> paginationDataFetcher;

    public DXPaginatedDataConnectionFetcher(DataFetcher<DXPaginatedData<T>> paginationDataFetcher) {
        this.paginationDataFetcher = paginationDataFetcher;
    }

    @Override
    public Connection<T> get(DataFetchingEnvironment environment) {
        DXPaginatedData<T> paginatedData = paginationDataFetcher.get(environment);
        if (paginatedData == null) {
            return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null,null,false,false));
        }
        List<Edge<T>> edges = buildEdges(paginatedData);
        PageInfo pageInfo = getPageInfo(edges, paginatedData);
        return new DXConnection<T>(edges, pageInfo);
    }

    private DXPageInfo getPageInfo(List<Edge<T>> edges, DXPaginatedData<T> paginatedData) {
        return new DXPageInfo(
                edges.size() > 0 ? edges.get(0).getCursor() : null,
                edges.size() > 0 ? edges.get(edges.size() - 1).getCursor() : null,
                paginatedData.hasPreviousPage(),
                paginatedData.hasNextPage(),
                paginatedData.getNodesCount(),
                paginatedData.getTotalCount());
    }

    private List<Edge<T>> buildEdges(DXPaginatedData<T> paginatedData) {
        Iterator<T> data = paginatedData.iterator();
        List<Edge<T>> edges = new ArrayList<>();
        for (; data.hasNext(); ) {
            T entity = data.next();
            edges.add(new DXEdge<>(entity, paginatedData.getIndex(entity), new DefaultConnectionCursor(paginatedData.getCursor(entity))));
        }
        return edges;
    }

}
