/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.collections4.Predicate;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataMutationException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an object, dealing with modification operations on a JCR node.
 */
@GraphQLName("JCRNodeMutation")
@GraphQLDescription("Mutations on a JCR node")
public class GqlJcrNodeMutation extends GqlJcrMutationSupport {

    public JCRNodeWrapper jcrNode;

    public GqlJcrNodeMutation(JCRNodeWrapper node) {
        this.jcrNode = node;
    }

    @GraphQLField
    @GraphQLDescription("Get the graphQL representation of the node currently being mutated")
    public GqlJcrNode getNode() throws BaseGqlClientException {
        try {
            return SpecializedTypesHandler.getNode(jcrNode);
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Get the identifier of the node currently being mutated")
    public String getUuid() throws BaseGqlClientException {
        try {
            return jcrNode.getIdentifier();
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the current node")
    public GqlJcrNodeMutation addChild(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create") String name,
                                       @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType)
    throws BaseGqlClientException {
        GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, null, null, null);
        return new GqlJcrNodeMutation(addNode(jcrNode, node));
    }

    @GraphQLField
    @GraphQLDescription("Batch creates a number of new JCR nodes under the current node")
    public Collection<GqlJcrNodeMutation> addChildrenBatch(@GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The collection of nodes to create") Collection<GqlJcrNodeInput> nodes) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (GqlJcrNodeInput node : nodes) {
            result.add(new GqlJcrNodeMutation(addNode(this.jcrNode, node)));
        }
        return result;
    }

    @GraphQLField
    @GraphQLDescription("Mutates an existing sub node, based on its relative path to the current node")
    public GqlJcrNodeMutation mutateChild(@GraphQLName("path") @GraphQLNonNull @GraphQLDescription("Name or relative path of the sub node to mutate") String path) throws BaseGqlClientException {
        try {
            return new GqlJcrNodeMutation(jcrNode.getNode(path));
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Mutates a set of existing sub nodes, based on filters passed as parameter")
    public Collection<GqlJcrNodeMutation> mutateChildren(@GraphQLName("names") @GraphQLDescription("Filter of child nodes by their names; null to avoid such filtering") Collection<String> names,
                                                   @GraphQLName("typesFilter") @GraphQLDescription("Filter of child nodes by their types; null to avoid such filtering") GqlJcrNode.NodeTypesInput typesFilter,
                                                   @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of child nodes by their property values; null to avoid such filtering") GqlJcrNode.NodePropertiesInput propertiesFilter)
    throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        Predicate<JCRNodeWrapper> predicate = NodeHelper.getNodesPredicate(names, typesFilter, propertiesFilter);
        try {
            for (JCRNodeWrapper node : jcrNode.getNodes()) {
                if (predicate.evaluate(node)) {
                    result.add(new GqlJcrNodeMutation(node));
                }
            }
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return result;
    }

    /**
     * Creates a mutation object for modifications of the specified node property.
     *
     * @param propertyName the name of the property to be modified
     * @return a mutation object for modifications of the specified node property
     */
    @GraphQLField
    @GraphQLDescription("Mutates or creates a property on the current node")
    public GqlJcrPropertyMutation mutateProperty(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property to update") String propertyName) {
        return new GqlJcrPropertyMutation(jcrNode, propertyName);
    }

    /**
     * Creates a collection of mutation object for modifications of the specified node properties
     *
     * @param names the name of node properties to be modified
     * @return a collection of mutation object for modifications of the specified node properties
     */
    @GraphQLField
    @GraphQLDescription("Mutates or creates a set of properties on the current node")
    public Collection<GqlJcrPropertyMutation> mutateProperties(@GraphQLName("names") @GraphQLDescription("The names of the JCR properties; null to obtain all properties") Collection<String> names) throws BaseGqlClientException {
        return names.stream().map((String name) -> new GqlJcrPropertyMutation(jcrNode, name)).collect(Collectors.toList());
    }

    /**
     * Performs batch-set of the specified properties on the JCR node.
     *
     * @param properties the collection of properties to be set
     * @return the collection of property mutation objects for the modified properties
     * @throws BaseGqlClientException in case of modification errors
     */
    @GraphQLField
    @GraphQLName("setPropertiesBatch")
    @GraphQLDescription("Mutates or creates a set of properties on the current node")
    public Collection<GqlJcrPropertyMutation> setPropertiesBatch(@GraphQLName("properties") @GraphQLDescription("The collection of JCR properties to set") Collection<GqlJcrPropertyInput> properties) throws BaseGqlClientException {
        return setProperties(jcrNode, properties).stream().map(GqlJcrPropertyMutation::new).collect(Collectors.toList());
    }

    /**
     * Adds the collection of mixin types for the current node.
     *
     * @param mixins the collection of mixin type names to be added
     * @return the collection of actual node mixin type names after the operation
     * @throws BaseGqlClientException in case of a mixin operation error
     */
    @GraphQLField
    @GraphQLDescription("Adds mixin types on the current node")
    public Collection<String> addMixins(@GraphQLName("mixins") @GraphQLNonNull @GraphQLDescription("The collection of mixin type names") Collection<String> mixins) throws BaseGqlClientException {
        try {
            for (String mixin : mixins) {
                jcrNode.addMixin(mixin);
            }
            return Arrays.stream(jcrNode.getMixinNodeTypes()).map(ExtendedNodeType::getName).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
    }

    /**
     * Removes the collection of mixin types from the current node.
     *
     * @param mixins the collection of mixin type names to be removed
     * @return the collection of actual node mixin type names after the operation
     * @throws BaseGqlClientException in case of a mixin operation error
     */
    @GraphQLField
    @GraphQLDescription("Removes mixin types on the current node")
    public Collection<String> removeMixins(@GraphQLName("mixins") @GraphQLNonNull @GraphQLDescription("The collection of mixin type names") Collection<String> mixins) throws BaseGqlClientException {
        try {
            for (String mixin : mixins) {
                jcrNode.removeMixin(mixin);
            }
            return Arrays.stream(jcrNode.getMixinNodeTypes()).map(ExtendedNodeType::getName).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Rename the current node")
    public String rename(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The new name of the node") String newName) throws BaseGqlClientException {
        try {
            jcrNode.rename(newName);
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return jcrNode.getPath();
    }

    @GraphQLField
    @GraphQLDescription("Move the current node")
    public String move(@GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The target node path or id") String parentPathOrId) throws BaseGqlClientException {
        try {
            JCRNodeWrapper parentDest = getNodeFromPathOrId(jcrNode.getSession(), parentPathOrId);
            jcrNode.getSession().move(jcrNode.getPath(), parentDest.getPath() + "/" + jcrNode.getName());
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return jcrNode.getPath();
    }

    /**
     * Deletes the current node (and its subgraph).
     *
     * @return operation result
     * @throws BaseGqlClientException in case of an error during node delete operation
     */
    @GraphQLField
    @GraphQLDescription("Delete the current node (and its subgraph)")
    public boolean delete() throws BaseGqlClientException {
        try {
            jcrNode.remove();
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return true;
    }

    /**
     * Marks this node (and all the sub-nodes) for deletion if the.
     *
     * @param comment the deletion comment
     * @return operation result
     * @throws BaseGqlClientException in case of an error during node mark for deletion operation
     */
    @GraphQLField
    @GraphQLDescription("Mark the current node (and its subgraph) for deletion")
    public boolean markForDeletion(@GraphQLName("comment") @GraphQLDescription("Optional deletion comment") String comment) throws BaseGqlClientException {
        try {
            jcrNode.markForDeletion(comment);
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return true;
    }

    /**
     * Unmarks this node and all the sub-nodes for deletion.
     *
     * @return operation result
     * @throws BaseGqlClientException in case of an error during this operation
     */
    @GraphQLField
    @GraphQLDescription("Unmark this node and all the sub-nodes for deletion")
    public boolean unmarkForDeletion() throws BaseGqlClientException {
        try {
            jcrNode.unmarkForDeletion();
        } catch (RepositoryException e) {
            throw new DataMutationException(e);
        }
        return true;
    }
}
