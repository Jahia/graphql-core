/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclService;
import org.jahia.modules.graphql.provider.dxm.image.GqlJcrImageTransformMutation;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.predicate.PredicateHelper;
import org.jahia.modules.graphql.provider.dxm.user.PrincipalType;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRVersionService;
import org.jahia.services.content.nodetypes.ExtendedNodeType;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionManager;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Represents an object, dealing with modification operations on a JCR node.
 */
@GraphQLName("JCRNodeMutation")
@GraphQLDescription("Mutations on a JCR node")
public class GqlJcrNodeMutation extends GqlJcrMutationSupport {

    @Inject
    @GraphQLOsgiService
    private JahiaAclService aclService;

    /**
     * The target position of reordered child nodes.
     */
    @GraphQLDescription("The target position of reordered child nodes")
    public enum ReorderedChildrenPosition {
        /**
         * Specified children are sorted in a requested order and placed at the end of the list after the remaining children.
         */
        @GraphQLDescription("Specified children are sorted in a requested order and placed at the end of the list after the remaining children")
        LAST,

        /**
         * Specified children are sorted in a requested order, but remaining are kept at their places.
         */
        @GraphQLDescription("Specified children are sorted in a requested order, but remaining are kept at their places")
        INPLACE,

        /**
         * Specified children are sorted in a requested order and placed at the top, before all remaining children.
         */
        @GraphQLDescription("Specified children are sorted in a requested order and placed at the top, before all remaining children")
        FIRST;
    }

    /**
     * Default value supplier for {@link ReorderedChildrenPosition}.
     */
    public static class ReorderedChildrenPositionDefaultValue implements Supplier<Object> {

        @Override
        public ReorderedChildrenPosition get() {
            return ReorderedChildrenPosition.INPLACE;
        }
    }

    public JCRNodeWrapper jcrNode;

    public GqlJcrNodeMutation(JCRNodeWrapper node) {
        this.jcrNode = node;
    }

    @GraphQLField
    @GraphQLName("node")
    @GraphQLDescription("Get the graphQL representation of the node currently being mutated")
    public GqlJcrNode getNode() throws BaseGqlClientException {
        try {
            return SpecializedTypesHandler.getNode(jcrNode);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("uuid")
    @GraphQLDescription("Get the identifier of the node currently being mutated")
    public String getUuid() throws BaseGqlClientException {
        try {
            return jcrNode.getIdentifier();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Adds child node for the current one.
     *
     * @param name the name of the child node to be added
     * @param primaryNodeType the primary node type of the child
     * @param mixins collection of mixin types, which should be added to the created node
     * @param properties collection of properties to be set on the newly created node
     * @param children collection of child nodes to be added to the newly created node
     * @return a mutation object for the created child node
     * @throws BaseGqlClientException in case of creation operation error
     */
    @GraphQLField
    @GraphQLDescription("Creates a new JCR node under the current node")
    public GqlJcrNodeMutation addChild(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create") String name,
                                       @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType,
                                       @GraphQLName("useAvailableNodeName") @GraphQLDescription("If true, use the next available name for a node, appending if needed numbers. Default is false") Boolean useAvailableNodeName,
                                       @GraphQLName("mixins") @GraphQLDescription("The collection of mixin type names") Collection<String> mixins,
                                       @GraphQLName("properties") Collection<GqlJcrPropertyInput> properties,
                                       @GraphQLName("children") Collection<GqlJcrNodeInput> children)
    throws BaseGqlClientException {
        GqlJcrNodeInput node = new GqlJcrNodeInput(name, primaryNodeType, useAvailableNodeName, mixins, properties, children);
        return new GqlJcrNodeMutation(addNode(jcrNode, node));
    }

    /**
     * Adds multiple child nodes for the current one.
     *
     * @param nodes the list of child nodes to be added
     * @return a collection of mutation objects for created children
     * @throws BaseGqlClientException in case of creation operation error
     */
    @GraphQLField
    @GraphQLDescription("Batch creates a number of new JCR nodes under the current node")
    public Collection<GqlJcrNodeMutation> addChildrenBatch(@GraphQLName("nodes") @GraphQLNonNull @GraphQLDescription("The collection of nodes to create") Collection<GqlJcrNodeInput> nodes) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> result = new ArrayList<>();
        for (GqlJcrNodeInput node : nodes) {
            result.add(new GqlJcrNodeMutation(addNode(this.jcrNode, node)));
        }
        return result;
    }

    /**
     * Import a file under the current node.
     *
     * @param file name of the request part that contains desired import file body
     * @param environment data fetching environment
     * @return always true
     * @throws BaseGqlClientException in case of errors during import operation
     */
    @GraphQLField
    @GraphQLDescription("Import a file under the current node")
    public boolean importContent(
        @GraphQLName("file") @GraphQLNonNull @GraphQLDescription("Name of the request part that contains desired import file body") String file,
        DataFetchingEnvironment environment
    ) throws BaseGqlClientException {
        importFileUpload(file, this.jcrNode, environment);
        return true;
    }

    /**
     * Creates a mutation object for modifications of a node's descendant.
     *
     * @param relPath the relative path of the child node to retrieve
     * @return a mutation object for modifications of a node's child
     * @throws BaseGqlClientException in case of an error during retrieval of child node
     */
    @GraphQLField
    @GraphQLDescription("Mutates an existing sub node, based on its relative path to the current node")
    public GqlJcrNodeMutation mutateDescendant(@GraphQLName("relPath") @GraphQLNonNull @GraphQLDescription("Name or relative path of the sub node to mutate") String relPath) throws BaseGqlClientException {
        if (relPath.contains("..")) {
            throw new GqlJcrWrongInputException("No navigation outside of the node sub-tree is supported");
        }
        try {
            return new GqlJcrNodeMutation(jcrNode.getNode(relPath));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Creates a collection of mutation object to modify the node descendants.
     *
     * @param typesFilter filter of descendant nodes by their types; <code>null</code> to avoid such filtering
     * @param propertiesFilter filter of descendant nodes by their property values; <code>null</code> to avoid such filtering
     * @return a collection of mutation object to modify the node descendants
     * @throws BaseGqlClientException in case of descendant retrieval error
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing descendant nodes, based on filters passed as parameter")
    public Collection<GqlJcrNodeMutation> mutateDescendants(@GraphQLName("typesFilter") @GraphQLDescription("Filter of descendant nodes by their types; null to avoid such filtering") GqlJcrNode.NodeTypesInput typesFilter,
                                                            @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of descendant nodes by their property values; null to avoid such filtering") GqlJcrNode.NodePropertiesInput propertiesFilter,
                                                            @GraphQLName("recursionTypesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their types; null to avoid such filtering") GqlJcrNode.NodeTypesInput recursionTypesFilter,
                                                            @GraphQLName("recursionPropertiesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their property values; null to avoid such filtering") GqlJcrNode.NodePropertiesInput recursionPropertiesFilter) throws BaseGqlClientException {
        List<GqlJcrNodeMutation> descendants = new LinkedList<>();
        try {
            NodeHelper.collectDescendants(jcrNode, NodeHelper.getNodesPredicate(null, typesFilter, propertiesFilter, null, null),  NodeHelper.getNodesPredicate(null, recursionTypesFilter, recursionPropertiesFilter, null, null),  descendant -> descendants.add(new GqlJcrNodeMutation(descendant)));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return descendants;
    }

    /**
     * Creates a collection of mutation object to modify the direct children nodes.
     *
     * @param names filter of child nodes by their names; <code>null</code> to avoid such filtering
     * @param typesFilter filter of child nodes by their types; <code>null</code> to avoid such filtering
     * @param propertiesFilter filter of child nodes by their property values; <code>null</code> to avoid such filtering
     * @return a collection of mutation object to modify the direct children nodes
     * @throws BaseGqlClientException in case of children retrieval error
     */
    @GraphQLField
    @GraphQLDescription("Mutates a set of existing direct sub nodes, based on filters passed as parameter")
    public Collection<GqlJcrNodeMutation> mutateChildren(@GraphQLName("names") @GraphQLDescription("Filter of child nodes by their names; null to avoid such filtering") Collection<String> names,
                                                         @GraphQLName("typesFilter") @GraphQLDescription("Filter of child nodes by their types; null to avoid such filtering") GqlJcrNode.NodeTypesInput typesFilter,
                                                         @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of child nodes by their property values; null to avoid such filtering") GqlJcrNode.NodePropertiesInput propertiesFilter)
    throws BaseGqlClientException {
        List<GqlJcrNodeMutation> children = new LinkedList<>();
        try {
            NodeHelper.collectDescendants(jcrNode, NodeHelper.getNodesPredicate(names, typesFilter, propertiesFilter, null, null), PredicateHelper.falsePredicate(), child -> children.add(new GqlJcrNodeMutation(child)));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return children;
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
    public Collection<GqlJcrPropertyMutation> mutateProperties(@GraphQLName("names") @GraphQLDescription("The names of the JCR properties; null to obtain all properties") Collection<String> names) {
        return names.stream().map((String name) -> new GqlJcrPropertyMutation(jcrNode, name)).collect(Collectors.toList());
    }

    /**
     * Performs batch-delete of the specified properties on the JCR node.
     *
     * @param properties the collection of properties to be deleted
     * @return Boolean
     * @throws BaseGqlClientException in case of modification errors
     */
    @GraphQLField
    @GraphQLName("deletePropertiesBatch")
    @GraphQLDescription("Deletes a set of properties on the current node")
    public Boolean deletePropertiesBatch(@GraphQLName("properties") @GraphQLDescription("The collection of JCR properties: name and language") Collection<GqlJcrDeletedPropertyInput> properties) {
        return deleteProperties(jcrNode, properties);
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
            throw new DataFetchingException(e);
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
            throw new DataFetchingException(e);
        }
    }

    /**
     * Renames the current node.
     *
     * @param newName the new name for the node
     * @return the new full path of the renamed node
     * @throws BaseGqlClientException in case of renaming error
     */
    @GraphQLField
    @GraphQLDescription("Rename the current node")
    public String rename(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The new name of the node") String newName) throws BaseGqlClientException {
        try {
            jcrNode.rename(newName);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return jcrNode.getPath();
    }

    /**
     * Moves the current node to a specified destination path (if <code>destPath</code> is specified) or moves it under the specified node
     * (if <code>parentPathOrId</code> is specified). Either of two parameters is expected.
     *
     * @param destPath target node path of the current node after the move operation
     * @param parentPathOrId parent node path or id under which the current node will be moved to
     * @param renameOnConflict boolean to indicate if source node needs to be renamed
     * @return the new path of the current node after move operation
     * @throws BaseGqlClientException in case of move error
     */
    @GraphQLField
    @GraphQLDescription("Moves the current node to a specified destination path (if destPath is specified) or moves it under the specified node"
            + " (if parentPathOrId is specified). Either of two parameters is expected.")
    public String move(@GraphQLName("destPath") @GraphQLDescription("The target node path of the current node after the move operation") String destPath,
                       @GraphQLName("parentPathOrId") @GraphQLDescription("The parent node path or id under which the current node will be moved to") String parentPathOrId,
                       @GraphQLName("renameOnConflict") @GraphQLDescription("Renames current node before moving if the name exists under destination node") Boolean renameOnConflict) throws BaseGqlClientException {
        try {
            JCRSessionWrapper session = jcrNode.getSession();
            if (destPath != null) {
                if (renameOnConflict != null && renameOnConflict && session.itemExists(destPath) && session.getNode(destPath).hasNode(jcrNode.getName())) {
                    safeRename(session.getNode(destPath), jcrNode.getName());
                }

                jcrNode.getSession().move(jcrNode.getPath(), destPath);
            } else if (parentPathOrId != null) {
                JCRNodeWrapper parentDest = getNodeFromPathOrId(jcrNode.getSession(), parentPathOrId);
                if (renameOnConflict != null && renameOnConflict && parentDest.hasNode(jcrNode.getName())) {
                    safeRename(parentDest, jcrNode.getName());
                }

                String dest = parentDest.getPath() + "/" + jcrNode.getName();

                jcrNode.getSession().move(jcrNode.getPath(), dest);
                // The node can be moved into a different provider, so we need to update it
                jcrNode = jcrNode.getSession().getNode(dest);
            } else {
                throw new GqlJcrWrongInputException(
                        "Either destPath or parentPathOrId is expected for the node move operation");
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
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
            throw new DataFetchingException(e);
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
            throw new DataFetchingException(e);
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
            throw new DataFetchingException(e);
        }
        return true;
    }

    /**
     * Return image transformation mutation
     *
     * @return image transformation mutation
     * @throws BaseGqlClientException in case of an error during this operation
     */
    @GraphQLField
    @GraphQLDescription("Return image transformation mutation")
    public GqlJcrImageTransformMutation transformImage(@GraphQLName("name") @GraphQLDescription("name of target file, if different") String name,
                                                       @GraphQLName("targetPath") @GraphQLDescription("target path, if different") String targetPath) throws BaseGqlClientException {
        try {
            if (jcrNode.isNodeType("jmix:image")) {
                return new GqlJcrImageTransformMutation(jcrNode, name != null ? JCRContentUtils.escapeLocalNodeName(name) : null, targetPath != null ? JCRContentUtils.escapeNodePath(targetPath) : null);
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return null;
    }

    /**
     * Return zip mutation
     * @return GqlZipMutation or throws an exception in case the path specified by the user is not a file
     * @throws BaseGqlClientException
     */
    @GraphQLField
    @GraphQLDescription("Return zip mutation")
    public GqlZipMutation zip() throws DataFetchingException {
        if (jcrNode.isFile()) {
            return new GqlZipMutation(jcrNode);
        } else {
            throw new DataFetchingException(jcrNode.getPath() + " is not a file");
        }
    }

    /**
     * Reorder child nodes according to the list of names passed. The result of reordering will ensure that the ordering of specified child
     * nodes will be guaranteed. But the resulting position of specified set of ordered children will depend on the value of the
     * <code>position</code> parameter (see {@link ReorderedChildrenPosition}).
     *
     * @param names a non-empty list of child node names in the desired order
     * @param position see {@link ReorderedChildrenPosition} for possible values. The default value is <code>inplace</code>.
     * @return operation result
     * @throws BaseGqlClientException in case of an error
     */
    @GraphQLField
    @GraphQLDescription("Reorder child nodes according to the list of names passed")
    public boolean reorderChildren(
            @GraphQLName("names") @GraphQLNonNull @GraphQLDescription("List of child node names in the desired order") List<String> names,
            @GraphQLName("position") @GraphQLDefaultValue(ReorderedChildrenPositionDefaultValue.class) @GraphQLDescription("The target position of reordered child nodes. The default value is inplace.") ReorderedChildrenPosition position)
    throws BaseGqlClientException {

        validateChildNamesToReorder(names, position);

        try {
            String destChildName = null;
            //Avoid potential issues with ImmutableList
            List<String> reversedNames = new ArrayList<>(names);
            Collections.reverse(reversedNames);
            // we proceed in reverse order
            for (String srcChildName : reversedNames) {
                if (destChildName == null) {
                    // we are on the last element in the list (first one, we process)
                    if (position == ReorderedChildrenPosition.LAST) {
                        // we put it at the end of the children list
                        jcrNode.orderBefore(srcChildName, null);
                    } else if (position == ReorderedChildrenPosition.FIRST) {
                        // we put it at the beginning
                        Iterator<JCRNodeWrapper> childNodeIterator = jcrNode.getNodes().iterator();
                        if (childNodeIterator.hasNext()) {
                            jcrNode.orderBefore(srcChildName, childNodeIterator.next().getName());
                        } else {
                            throw new GqlJcrWrongInputException(
                                    "Node " + jcrNode.getPath() + " has no children to reorder");
                        }
                    }
                } else {
                    jcrNode.orderBefore(srcChildName, destChildName);
                }
                destChildName = srcChildName;
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return true;
    }

    @GraphQLField
    @GraphQLDescription("Grant role permissions to specified principal user/group for the given node")
    public boolean grantRoles(
            @GraphQLName("roleNames") @GraphQLDescription("Roles to grant user/group for this node") @GraphQLNonNull List<String> roleNames,
            @GraphQLName("principalType") @GraphQLDescription("Type of principal (user/group) specified") @GraphQLNonNull PrincipalType principalType,
            @GraphQLName("principalName") @GraphQLDescription("Name of principal (user/group)") @GraphQLNonNull String principalName
    ) throws RepositoryException {
        String principalKey = principalType.getPrincipalKey(principalName);
        return aclService.grantRoles(jcrNode, principalKey, roleNames);
    }

    @GraphQLField
    @GraphQLDescription("Remove/deny roles to specified principal user/group for the given node")
    public boolean revokeRoles(
            @GraphQLName("roleNames") @GraphQLDescription("Roles to grant user/group for this node") @GraphQLNonNull List<String> roleNames,
            @GraphQLName("principalType") @GraphQLDescription("Type of principal (user/group) specified") @GraphQLNonNull PrincipalType principalType,
            @GraphQLName("principalName") @GraphQLDescription("Name of principal (user/group)") @GraphQLNonNull String principalName
    ) throws RepositoryException {
        String principalKey = principalType.getPrincipalKey(principalName);
        return aclService.revokeRoles(jcrNode, principalKey, roleNames);
    }

    @GraphQLField
    @GraphQLDescription("Create new version for the node if the node supports versioning")
    public boolean createVersion() throws DataFetchingException {
        FastDateFormat DF = FastDateFormat.getInstance("yyyy_MM_dd_HH_mm_ss");
        JCRVersionService versionService = JCRVersionService.getInstance();

        try {
            boolean supportVersioning = jcrNode.getProvider().getRepository().getDescriptorValue(Repository.OPTION_VERSIONING_SUPPORTED).getBoolean();
            if(supportVersioning) {
                JCRSessionWrapper session = jcrNode.getSession();
                session.save();
                VersionManager versionManager = session.getWorkspace().getVersionManager();
                String label = "uploaded_at_" + DF.format(jcrNode.getProperty("jcr:lastModified").getDate().getTime().getTime());
                if (!jcrNode.isVersioned()) {
                    jcrNode.versionFile();
                    session.save();
                }
                versionManager.checkout(jcrNode.getPath());
                session.getWorkspace().getVersionManager().checkpoint(jcrNode.getPath());
                versionService.addVersionLabel(jcrNode, label);
                return true;
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return false;
    }

    private void validateChildNamesToReorder(List<String> names, ReorderedChildrenPosition position) {
        if (names == null || names.isEmpty()) {
            // check for presence of names
            throw new GqlJcrWrongInputException("A non-empty list of child node names is expected");
        }
        if (names.size() == 1) {
            // we've got a single name
            if (position == ReorderedChildrenPosition.INPLACE) {
                throw new GqlJcrWrongInputException(
                        "Reorder operation expects at least two names in case target position is inplace");
            }
        } else {
            // check for duplicate names
            Set<String> uniqueNames = new LinkedHashSet<>(names);
            if (uniqueNames.size() != names.size()) {
                throw new GqlJcrWrongInputException(
                        "Ambigous child name order: duplicates are not expected in the list of passed child node names to reorder");
            } else {
                // check for null
                if (names.indexOf(null) != -1 || names.indexOf(StringUtils.EMPTY) != -1) {
                    // null is not permitted
                    throw new GqlJcrWrongInputException("Null or empty child names are not permitted");
                }
            }
        }
    }

    private void safeRename(JCRNodeWrapper destNode, String desiredName) throws RepositoryException {
        boolean nameFound = false;

        // Go back and forth between dest and source and try to find suitable name
        while(!nameFound) {
            if (destNode.hasNode(desiredName)) {
                desiredName = JCRContentUtils.findAvailableNodeName(destNode, desiredName);
            }
            nameFound = true;
            if (jcrNode.getParent() != null && jcrNode.getParent().hasNode(desiredName)) {
                desiredName = JCRContentUtils.findAvailableNodeName(jcrNode.getParent(), desiredName);
                nameFound = false;
            }
        }

        jcrNode.rename(desiredName);
        jcrNode.getSession().save();
    }
}
