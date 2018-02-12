package org.jahia.modules.graphql.provider.dxm.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;

/**
 * Contains resources commonly used by GraphQL JCR mutations internally.
 */
public class GqlJcrMutationSupport {

    /**
     * Add a child node to the specified one.
     *
     * @param parent The JCR node to add a child to
     * @param node GraphQL representation of the child node to be added
     * @return The child JCR node that was added
     * @throws RepositoryException in case of a JCR error during add operation
     */
    protected static JCRNodeWrapper internalAddNode(JCRNodeWrapper parent, GqlJcrNodeInput node) throws RepositoryException {
        JCRNodeWrapper jcrNode = parent.addNode(node.getName(), node.getPrimaryNodeType());
        if (node.getMixins() != null) {
            for (String mixin : node.getMixins()) {
                jcrNode.addMixin(mixin);
            }
        }
        if (node.getProperties() != null) {
            internalSetProperties(jcrNode, node.getProperties());
        }
        if (node.getChildren() != null) {
            for (GqlJcrNodeInput child : node.getChildren()) {
                internalAddNode(jcrNode, child);
            }
        }
        return jcrNode;
    }

    /**
     * Set the provided properties to the specified node.
     *
     * @param node The JCR node to set properties to
     * @param Properties the collection of properties to be set
     * @return The result of the operation, containing list of modified JCR properties
     * @throws RepositoryException in case of a JCR error during node update
     */
    protected static List<JCRPropertyWrapper> internalSetProperties(JCRNodeWrapper node, Collection<GqlJcrPropertyInput> properties) throws RepositoryException {
        List<JCRPropertyWrapper> result = new ArrayList<>();
        for (GqlJcrPropertyInput property : properties) {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, property.getLanguage());
            JCRSessionWrapper session = localizedNode.getSession();
            int type = (property.getType() != null ? property.getType().getValue() : PropertyType.STRING);
            if (property.getValue() != null) {
                Value v = session.getValueFactory().createValue(property.getValue(), type);
                result.add(localizedNode.setProperty(property.getName(), v));
            } else if (property.getValues() != null) {
                List<Value> values = new ArrayList<>();
                for (String value : property.getValues()) {
                    values.add(session.getValueFactory().createValue(value, type));
                }
                result.add(localizedNode.setProperty(property.getName(), values.toArray(new Value[values.size()])));
            }
        }
        return result;
    }

    /**
     * Retrieve the specified JCR node by its path or UUID.
     *
     * @param session JCR session to be used for node retrieval
     * @param pathOrId The string with either node UUID or its path
     * @return The requested JCR node
     * @throws RepositoryException in case of node retrieval operation
     */
    protected static JCRNodeWrapper getNodeFromPathOrId(JCRSessionWrapper session, String pathOrId) throws RepositoryException {
        return ('/' == pathOrId.charAt(0) ? session.getNode(pathOrId) : session.getNodeByIdentifier(pathOrId));
    }
}
