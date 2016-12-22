package org.jahia.modules.graphql.provider.dxm.model;

import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * TODO Comment me
 *
 * @author toto
 */
public class DXGraphQLNode {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNode.class);
    private JCRNodeWrapper node;
    private String type;

    private String identifier;
    private String name;
    private String path;
    private String parentIdentifier;
    private String parentPath;

    public DXGraphQLNode(JCRNodeWrapper node) {
        this(node, null);
    }

    public DXGraphQLNode(JCRNodeWrapper node, String forcedType) {
        this.node = node;
        try {
            this.identifier = node.getIdentifier();
            this.name = node.getName();
            this.path = node.getPath();
            if (forcedType != null && node.isNodeType(forcedType)) {
                this.type = forcedType;
            } else {
                this.type = node.getPrimaryNodeTypeName();
            }
            if (!node.getPath().equals("/")) {
                this.parentPath = node.getParent().getPath();
                this.parentIdentifier = node.getParent().getIdentifier();
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public JCRNodeWrapper getNode() {
        return node;
    }

    public String getType() {
        return type;
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
