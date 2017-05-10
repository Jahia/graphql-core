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
    private String systemId;
    private boolean isMixin;
    private boolean isAbstract;
    private boolean hasOrderableChildNodes;
    private boolean isQueryable;

    public DXGraphQLNodeType(ExtendedNodeType nodeType) {
        this.nodeType = nodeType;
        this.name = nodeType.getName();
        this.systemId = nodeType.getSystemId();
        this.isMixin = nodeType.isMixin();
        this.isAbstract = nodeType.isAbstract();
        this.hasOrderableChildNodes = nodeType.hasOrderableChildNodes();
        this.isQueryable = nodeType.isQueryable();
    }

    public ExtendedNodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public String getSystemId() {
        return systemId;
    }

    public boolean isMixin() {
        return isMixin;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isHasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    public boolean isQueryable() {
        return isQueryable;
    }
}
