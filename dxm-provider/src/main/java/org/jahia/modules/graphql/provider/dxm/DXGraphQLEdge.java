package org.jahia.modules.graphql.provider.dxm;

/**
 * Created by toto on 15/06/17.
 */
public class DXGraphQLEdge<T> {
    private T node;
    private String cursor;
    private int offset;

    public DXGraphQLEdge(T node, String cursor, int offset) {
        this.node = node;
        this.cursor = cursor;
        this.offset = offset;
    }

    public T getNode() {
        return node;
    }

    public String getCursor() {
        return cursor;
    }

    public int getOffset() {
        return offset;
    }
}
