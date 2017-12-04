package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.modules.graphql.provider.dxm.relay.CursorSupport;

import javax.jcr.RepositoryException;
import java.nio.charset.StandardCharsets;

import static java.util.Base64.getEncoder;

public class NodeCursor implements CursorSupport<GqlJcrNode> {

    private static NodeCursor instance = new NodeCursor();

    public static NodeCursor getInstance() {
        return instance;
    }

    @Override
    public String getCursor(GqlJcrNode entity) {
        try {
            byte[] bytes = entity.getNode().getIdentifier().getBytes(StandardCharsets.UTF_8);
            return getEncoder().encodeToString(bytes);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
