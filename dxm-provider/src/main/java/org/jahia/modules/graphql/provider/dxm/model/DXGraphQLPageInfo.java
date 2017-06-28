package org.jahia.modules.graphql.provider.dxm.model;

import com.google.common.collect.Iterables;

/**
 * Created by toto on 15/06/17.
 */
public class DXGraphQLPageInfo {

    private DXGraphQLConnection<?> connection;

    public DXGraphQLPageInfo(DXGraphQLConnection<?> connection) {
        this.connection = connection;
    }

    public String getStartCursor() {
        return connection.getEdges().isEmpty() ? null : connection.getEdges().get(0).getCursor();
    }

    public String getEndCursor() {
        return connection.getEdges().isEmpty() ? null : connection.getEdges().get(connection.getEdges().size()-1).getCursor();
    }

    public boolean isHasPreviousPage() {
        return !connection.getEdges().isEmpty() && connection.getEdges().get(0).getOffset() > 0;
    }

    public boolean isHasNextPage() {
        return !connection.getEdges().isEmpty() && connection.getEdges().get(connection.getEdges().size()-1).getOffset() < getTotalCount()-1;
    }

    public Integer getStartOffset() {
        return connection.getEdges().isEmpty() ? null : connection.getEdges().get(0).getOffset();
    }

    public Integer getEndOffset() {
        return connection.getEdges().isEmpty() ? null : connection.getEdges().get(connection.getEdges().size()-1).getOffset();
    }

    public int getCount() {
        return connection.getEdges().size();
    }

    public int getTotalCount() {
        return Iterables.size(connection.getIterable());
    }
}
