/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FilterHelper;
import org.jahia.modules.graphql.provider.dxm.predicate.MulticriteriaEvaluation;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.services.content.*;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Predicate;

/**
 * GraphQL representation of a JCR node - generic implementation.
 */
@GraphQLName("GenericJCRNode")
public class GqlJcrNodeImpl implements GqlJcrNode {

    public static final List<String> DEFAULT_EXCLUDED_CHILDREN = Arrays.asList("jnt:translation");
    public static final Predicate<JCRNodeWrapper> DEFAULT_CHILDREN_PREDICATE = NodeHelper.getTypesPredicate(new NodeTypesInput(MulticriteriaEvaluation.NONE, DEFAULT_EXCLUDED_CHILDREN));

    private JCRNodeWrapper node;
    private String type;

    /**
     * Create an instance that represents a JCR node to GraphQL.
     *
     * @param node The JCR node to represent
     */
    public GqlJcrNodeImpl(JCRNodeWrapper node) {
        this(node, null);
    }

    /**
     * Create an instance that represents a JCR node to GraphQL as a given node type.
     *
     * @param node The JCR node to represent
     * @param type The type name to represent the node as, or null to represent as node's primary type
     */
    public GqlJcrNodeImpl(JCRNodeWrapper node, String type) {
        this.node = node;
        if (type != null) {
            this.type = type;
        } else {
            try {
                this.type = node.getPrimaryNodeTypeName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public JCRNodeWrapper getNode() {
        return node;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    @GraphQLNonNull
    public String getUuid() {
        try {
            return node.getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public NodeQueryExtensions.Workspace getWorkspace() {
        try {
            return Constants.LIVE_WORKSPACE.equals(node.getSession().getWorkspace().getName()) ? NodeQueryExtensions.Workspace.LIVE : NodeQueryExtensions.Workspace.EDIT;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public String getName() {
        return node.getName();
    }

    @Override
    @GraphQLNonNull
    public String getPath() {
        return node.getPath();
    }

    @Override
    public String getDisplayName(@GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node, language);
            return node.getDisplayableName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GqlJcrNode getParent() {
        try {
            return SpecializedTypesHandler.getNode(node.getParent());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public Collection<GqlJcrProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                                    @GraphQLName("language") String language,
                                                    @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter, DataFetchingEnvironment environment) {
        List<GqlJcrProperty> properties = new LinkedList<GqlJcrProperty>();
        try {
            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node, language);
            if (names != null) {
                for (String name : names) {
                    if (node.hasProperty(name)) {
                        properties.add(new GqlJcrProperty(node.getProperty(name), this));
                    }
                }
            } else {
                for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) it.nextProperty();
                    properties.add(new GqlJcrProperty(property, this));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return FilterHelper.filterList(properties, fieldFilter, environment);
    }

    @Override
    public GqlJcrProperty getProperty(@GraphQLName("name") @GraphQLNonNull String name,
                                      @GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node, language);
            if (!node.hasProperty(name)) {
                return null;
            }
            return new GqlJcrProperty(node.getProperty(name), this);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    @GraphQLNonNull
    public DXPaginatedData<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                                   @GraphQLName("typesFilter") NodeTypesInput typesFilter,
                                                   @GraphQLName("propertiesFilter") NodePropertiesInput propertiesFilter,
                                                   @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter,
                                                   @GraphQLName("includesSelf") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean includesSelf,
                                                   DataFetchingEnvironment environment) {
        try {
            return NodeHelper.getPaginatedNodesList(node.getNodes(), typesFilter, propertiesFilter, fieldFilter, environment);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GqlJcrNode getDescendant(@GraphQLName("relPath") @GraphQLNonNull String relPath) {
        if (relPath.contains("..")) {
            throw new GqlJcrWrongInputException("No navigation outside of the node sub-tree is supported");
        }
        try {
            if (node.hasNode(relPath)) {
                return SpecializedTypesHandler.getNode(node.getNode(relPath));
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return null;
    }

    @Override
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    @GraphQLNonNull
    public DXPaginatedData<GqlJcrNode> getDescendants(@GraphQLName("typesFilter") NodeTypesInput typesFilter,
                                                      @GraphQLName("propertiesFilter") NodePropertiesInput propertiesFilter,
                                                      @GraphQLName("recursionTypesFilter") NodeTypesInput recursionTypesFilter,
                                                      @GraphQLName("recursionPropertiesFilter") NodePropertiesInput recursionPropertiesFilter,
                                                      @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter,
                                                      DataFetchingEnvironment environment) {
        try {
            DescendantsIterator it = new DescendantsIterator(node, NodeHelper.getNodesPredicate(null, recursionTypesFilter, recursionPropertiesFilter, environment));
            return NodeHelper.getPaginatedNodesList(it, typesFilter, propertiesFilter, fieldFilter, environment);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath,
                                         @GraphQLName("fieldFilter") FieldFiltersInput fieldFilter,
                                         DataFetchingEnvironment environment) {

        String upToPathNormalized;
        if (upToPath != null) {
            if (upToPath.isEmpty()) {
                throw new GqlJcrWrongInputException("'" + upToPath + "' is not a valid node path");
            }
            String nodePath = node.getPath();
            String nodePathNormalized = normalizePath(nodePath);
            upToPathNormalized = normalizePath(upToPath);
            if (nodePathNormalized.equals(upToPathNormalized) || !nodePathNormalized.startsWith(upToPathNormalized)) {
                throw new GqlJcrWrongInputException("'" + upToPath + "' does not reference an ancestor node of '" + nodePath + "'");
            }
        } else {
            upToPathNormalized = "/";
        }

        List<GqlJcrNode> ancestors = new LinkedList<GqlJcrNode>();
        try {
            for (JCRItemWrapper jcrAncestor : node.getAncestors()) {
                String ancestorPathNormalized = normalizePath(jcrAncestor.getPath());
                if (ancestorPathNormalized.startsWith(upToPathNormalized)) {
                    ancestors.add(SpecializedTypesHandler.getNode((JCRNodeWrapper) jcrAncestor));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return FilterHelper.filterList(ancestors, fieldFilter, environment);
    }

    @Override
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    @GraphQLNonNull
    public DXPaginatedData<GqlJcrProperty> getReferences(@GraphQLName("fieldFilter") FieldFiltersInput fieldFilter,
                                                         DataFetchingEnvironment environment) {
        List<GqlJcrProperty> references = new LinkedList<GqlJcrProperty>();
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        try {
            collectReferences(node.getReferences(), references, environment);
            collectReferences(node.getWeakReferences(), references, environment);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return PaginationHelper.paginate(FilterHelper.filterConnection(references, fieldFilter, environment), p -> PaginationHelper.encodeCursor(p.getNode().getUuid() + "/" + p.getName()), arguments);
    }

    private void collectReferences(PropertyIterator references, Collection<GqlJcrProperty> gqlReferences, DataFetchingEnvironment environment) throws RepositoryException {
        while (references.hasNext()) {
            JCRPropertyWrapper reference = (JCRPropertyWrapper) references.nextProperty();
            JCRNodeWrapper referencingNode = reference.getParent();
            if (PermissionHelper.hasPermission(referencingNode, environment)) {
                GqlJcrNode gqlReferencingNode = SpecializedTypesHandler.getNode(referencingNode);
                GqlJcrProperty gqlReference = gqlReferencingNode.getProperty(reference.getName(), reference.getLocale());
                gqlReferences.add(gqlReference);
            }
        }
    }

    @Override
    public GqlJcrNode getNodeInWorkspace(@GraphQLName("workspace") @GraphQLNonNull NodeQueryExtensions.Workspace workspace) {
        try {
            JCRNodeWrapper target = JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue()).getNodeByIdentifier(node.getIdentifier());
            return SpecializedTypesHandler.getNode(target);
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasPermission(@GraphQLName("permissionName") @GraphQLNonNull String permissionName) {
        return node.hasPermission(permissionName);
    }

    private static String normalizePath(String path) {
        return (path.endsWith("/") ? path : path + "/");
    }

    private class DescendantsIterator implements JCRNodeIteratorWrapper {
        private Stack<JCRNodeIteratorWrapper> its = new Stack<>();
        private Predicate<JCRNodeWrapper> recursionPredicate;
        private JCRNodeWrapper next = null;
        private boolean preloaded = false;
        private int position = 0;

        public DescendantsIterator(JCRNodeWrapper node, Predicate<JCRNodeWrapper> recursionPredicate) throws RepositoryException {
            this.its.push(node.getNodes());
            this.recursionPredicate = recursionPredicate;
        }

        private JCRNodeWrapper getNext(boolean forward) {
            if (!preloaded) {
                preloaded = true;
                try {
                    while (!its.isEmpty() && !its.peek().hasNext()) {
                        its.pop();
                    }
                    if (!its.isEmpty()) {
                        next = (JCRNodeWrapper) its.peek().nextNode();
                        if (recursionPredicate.test(next)) {
                            its.push(next.getNodes());
                        }
                    } else {
                        return null;
                    }
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
            if (forward) {
                preloaded = false;
            }
            return next;
        }

        @Override
        public Iterator<JCRNodeWrapper> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return getNext(false) != null;
        }

        @Override
        public JCRNodeWrapper next() {
            JCRNodeWrapper node = getNext(true);
            if (node == null) {
                throw new NoSuchElementException();
            }
            position ++;
            return node;
        }

        @Override
        public Node nextNode() {
            return next();
        }

        @Override
        public void skip(long skipNum) {
            for (int i = 0; i< skipNum; i++) {
                getNext(true);
            }
            if (node == null) {
                throw new NoSuchElementException();
            }
        }

        @Override
        public long getSize() {
            // Unknown size
            return -1;
        }

        @Override
        public long getPosition() {
            return position;
        }
    }
}
