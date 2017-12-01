package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.modules.graphql.provider.dxm.relay.CursorSupport;

import javax.jcr.RepositoryException;

public class NodeCursor implements CursorSupport<GqlJcrNode> {

    private static NodeCursor instance = new NodeCursor();

    public static NodeCursor getInstance() {
        return instance;
    }

    @Override
    public String getCursor(GqlJcrNode entity) {
        try {
            return entity.getNode().getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
