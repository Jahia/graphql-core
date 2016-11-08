package org.jahia.modules.graphql.provider.dxm.model;

import org.jahia.services.content.nodetypes.ExtendedNodeType;

/**
 * TODO Comment me
 *
 * @author toto
 */
public class DXGraphQLNodeType {
    private ExtendedNodeType nodeType;
    private String name;
    private boolean isMixin;
    private boolean isAbstract;

    public DXGraphQLNodeType(ExtendedNodeType nodeType) {
        this.nodeType = nodeType;
        this.name = nodeType.getName();
        this.isMixin = nodeType.isMixin();
        this.isAbstract = nodeType.isAbstract();
    }

    public ExtendedNodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public boolean isMixin() {
        return isMixin;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

}
