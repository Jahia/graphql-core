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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.jackrabbit.util.ISO8601;
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
import pl.touk.throwing.ThrowingFunction;
import pl.touk.throwing.ThrowingPredicate;
import pl.touk.throwing.ThrowingSupplier;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

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
            return NodeHelper.getPaginatedNodesList(node.getNodes(), names, typesFilter, propertiesFilter, fieldFilter, environment);
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
            JCRDescendantsNodeIterator it = new JCRDescendantsNodeIterator(node, NodeHelper.getNodesPredicate(null, recursionTypesFilter, recursionPropertiesFilter, environment));
            return NodeHelper.getPaginatedNodesList(it, null, typesFilter, propertiesFilter, fieldFilter, environment);
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

    @Override
    public String getAggregatedLastModifiedDate(@GraphQLName("language") String language, @GraphQLName("recursionTypesFilter") NodeTypesInput recursionTypesFilter, DataFetchingEnvironment environment) {
        try {
            JCRNodeWrapper i18node = NodeHelper.getNodeInLanguage(node, language);

            if (recursionTypesFilter == null) {
                // Default, do not recurse on sub pages
                recursionTypesFilter = new NodeTypesInput(MulticriteriaEvaluation.NONE, Collections.singleton(Constants.JAHIANT_PAGE));
            }

            Predicate<JCRNodeWrapper> predicate = NodeHelper.getTypesPredicate(recursionTypesFilter);
            JCRDescendantsNodeIterator it = new JCRDescendantsNodeIterator(i18node, predicate);

            JCRNodeWrapper max = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(predicate)
                    .filter(ThrowingPredicate.unchecked(n -> n.hasProperty(Constants.JCR_LASTMODIFIED)))
                    .reduce(i18node, (n1, n2) -> ThrowingSupplier.unchecked(() -> n1.getProperty(Constants.JCR_LASTMODIFIED).getLong() > n2.getProperty(Constants.JCR_LASTMODIFIED).getLong() ? n1 : n2).get());

            Calendar date = max.getProperty(Constants.JCR_LASTMODIFIED).getDate();
            return ISO8601.format(date);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getLanguagesToTranslate(@GraphQLName("languagesTranslated") List<String> languagesTranslated,
                                                @GraphQLName("languagesToCheck") List<String> languagesToCheck) {

        List<String> toBeTranslated = new ArrayList<>();
        try {
            if (CollectionUtils.isEmpty(languagesToCheck) || CollectionUtils.isEmpty(languagesTranslated) || node.getI18Ns().getSize() <= 0) {
                return toBeTranslated;
            }

            Optional<Long> lastTranslatedDate = languagesTranslated.stream()
                    .map(LocaleUtils::toLocale)
                    .filter(ThrowingPredicate.unchecked(localeTranslated -> node.hasI18N(localeTranslated)))
                    .map(ThrowingFunction.unchecked(localeTranslated -> node.getI18N(localeTranslated).getProperty(Constants.JCR_LASTMODIFIED).getLong()))
                    .max(Long::compareTo);

            if (lastTranslatedDate.isPresent()) {
                for (String languageToCheck : languagesToCheck) {
                    try {
                        Node translationNode = node.getI18N(LocaleUtils.toLocale(languageToCheck));
                        if (translationNode.getProperty(Constants.JCR_LASTMODIFIED).getLong() < lastTranslatedDate.get()) {
                            toBeTranslated.add(languageToCheck);
                        }
                    } catch (ItemNotFoundException e) {
                        toBeTranslated.add(languageToCheck);
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return toBeTranslated;
    }

    private static String normalizePath(String path) {
        return (path.endsWith("/") ? path : path + "/");
    }
}
