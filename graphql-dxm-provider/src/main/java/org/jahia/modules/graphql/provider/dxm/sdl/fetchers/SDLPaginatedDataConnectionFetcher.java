package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.annotations.connection.ConnectionFetcher;
import graphql.relay.*;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.relay.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SDLPaginatedDataConnectionFetcher<T> implements ConnectionFetcher<T> {

    private FinderListDataFetcher fetcher;

    public SDLPaginatedDataConnectionFetcher(FinderListDataFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public Connection<T> get(DataFetchingEnvironment environment) throws Exception {
        Stream<GqlJcrNode> l = fetcher.getStream(environment);
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        DXPaginatedData<GqlJcrNode> paginatedData = PaginationHelper.paginate(l, n -> PaginationHelper.encodeCursor(n.getUuid()), arguments);

        if (paginatedData == null) {
            return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null, null, false, false));
        }
        List<Edge<T>> edges = buildEdges((DXPaginatedData<T>) paginatedData);
        PageInfo pageInfo = getPageInfo(edges, (DXPaginatedData<T>) paginatedData);
        Class<? extends DXConnection<T>> connectionType = (Class<? extends DXConnection<T>>) DXGraphQLProvider.getInstance().getConnectionType(environment.getExecutionStepInfo().getFieldDefinition().getType().getName());
        if (connectionType != null) {
            try {
                return connectionType.getConstructor(List.class, PageInfo.class).newInstance(edges, pageInfo);
            } catch (ReflectiveOperationException e) {
                throw new DataFetchingException(e);
            }
        }
        return new DXConnection<>(edges, pageInfo);
    }

    private DXPageInfo getPageInfo(List<Edge<T>> edges, DXPaginatedData<T> paginatedData) {
        return new DXPageInfo(
                !edges.isEmpty() ? edges.get(0).getCursor() : null,
                !edges.isEmpty() ? edges.get(edges.size() - 1).getCursor() : null,
                paginatedData);
    }

    private List<Edge<T>> buildEdges(DXPaginatedData<T> paginatedData) {
        Iterator<T> data = paginatedData.iterator();
        List<Edge<T>> edges = new ArrayList<>();
        while (data.hasNext()) {
            T entity = data.next();
            edges.add(new DXEdge<>(entity, paginatedData.getIndex(entity), new DefaultConnectionCursor(paginatedData.getCursor(entity))));
        }
        return edges;
    }

}
