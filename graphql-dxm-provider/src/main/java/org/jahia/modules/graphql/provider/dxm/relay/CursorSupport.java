package org.jahia.modules.graphql.provider.dxm.relay;

public interface CursorSupport<T> {

    String getCursor(T obj);

}
