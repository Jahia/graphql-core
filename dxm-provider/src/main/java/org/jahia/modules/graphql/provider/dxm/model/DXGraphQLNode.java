package org.jahia.modules.graphql.provider.dxm.model;

import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

/**
 * TODO Comment me
 *
 * @author toto
 */
public class DXGraphQLNode {
    private JCRNodeWrapper node;

    private String identifier;
    private String name;
    private String path;
    private String parentIdentifier;
    private String parentPath;

    public DXGraphQLNode(JCRNodeWrapper node) {
        this.node = node;
        try {
            this.identifier = node.getIdentifier();
            this.name = node.getName();
            this.path = node.getPath();
            if (!node.getPath().equals("/")) {
                this.parentPath = node.getParent().getPath();
                this.parentIdentifier = node.getParent().getIdentifier();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public JCRNodeWrapper getNode() {
        return node;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public String getParentPath() {
        return parentPath;
    }
}
