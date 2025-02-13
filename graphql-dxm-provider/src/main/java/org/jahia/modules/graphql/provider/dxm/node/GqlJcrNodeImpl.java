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
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.acl.GqlAcl;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.modules.graphql.provider.dxm.util.ServletUtil;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.services.render.Template;
import org.jahia.services.seo.urlrewrite.UrlRewriteService;
import org.jahia.utils.LanguageCodeConverters;
import pl.touk.throwing.ThrowingFunction;
import pl.touk.throwing.ThrowingPredicate;
import pl.touk.throwing.ThrowingSupplier;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

import static org.jahia.modules.graphql.provider.dxm.node.NodeHelper.getTypesPredicate;


/**
 * GraphQL representation of a JCR node - generic implementation.
 */
@GraphQLName("GenericJCRNode")
@GraphQLDescription("GraphQL representation of a generic JCR node")
public class GqlJcrNodeImpl implements GqlJcrNode {

    public static final List<String> DEFAULT_EXCLUDED_CHILDREN = Arrays.asList("jnt:translation");
    public static final Predicate<JCRNodeWrapper> DEFAULT_CHILDREN_PREDICATE = getTypesPredicate(new NodeTypesInput(MulticriteriaEvaluation.NONE, DEFAULT_EXCLUDED_CHILDREN));

    private JCRNodeWrapper node;
    private String type;

    @Inject
    @GraphQLOsgiService
    private UrlRewriteService urlRewriteService;

    @Inject
    @GraphQLOsgiService
    private JCRTemplate jcrTemplate;

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

    private static String normalizePath(String path) {
        return (path.endsWith("/") ? path : path + "/");
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
    @GraphQLName("uuid")
    @GraphQLDescription("The UUID of the JCR node this object represents")
    public String getUuid() {
        try {
            return node.getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("workspace")
    @GraphQLNonNull
    @GraphQLDescription("Get the workspace of the query")
    public NodeQueryExtensions.Workspace getWorkspace() {
        try {
            return Constants.LIVE_WORKSPACE.equals(node.getSession().getWorkspace().getName()) ? NodeQueryExtensions.Workspace.LIVE : NodeQueryExtensions.Workspace.EDIT;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("The name of the JCR node this object represents")
    public String getName() {
        return node.getName();
    }

    @Override
    @GraphQLName("path")
    @GraphQLNonNull
    @GraphQLDescription("The path of the JCR node this object represents")
    public String getPath() {
        return node.getPath();
    }

    /**
     * @return The depth of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLName("depth")
    @GraphQLNonNull
    @GraphQLDescription("The depth in the JCR Tree of the JCR node this object represents")
    public Integer getDepth() {
        try {
            return node.getDepth();
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    };

    @Override
    @GraphQLName("displayName")
    @GraphQLDescription("The displayable name of the JCR node")
    public String getDisplayName(@GraphQLName("language") @GraphQLDescription("Language") String language) {
        try {
            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node, language);
            return node.getDisplayableName();
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("parent")
    @GraphQLDescription("GraphQL representation of the parent JCR node")
    public GqlJcrNode getParent() {
        try {
            return SpecializedTypesHandler.getNode(node.getParent());
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("properties")
    @GraphQLNonNull
    @GraphQLDescription("GraphQL representations of the properties in the requested language")
    public Collection<GqlJcrProperty> getProperties(
            @GraphQLName("names") @GraphQLDescription("The names of the JCR properties; null to obtain all properties") Collection<String> names,
            @GraphQLName("language") @GraphQLDescription("The language to obtain the properties in; must be a valid language code in case any internationalized properties are requested, does not matter for non-internationalized ones") String language,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
            @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("useFallbackLanguage") @GraphQLDescription("When set to true, returns the node in the default language if there is no translation for the requested language. Returns null if the option \"Replace untranslated content with the default language content\" is not activated for the site of the requested node. Will also return null if there is no translation for the default language.") Boolean useFallbackLanguage,
            DataFetchingEnvironment environment) {
        List<GqlJcrProperty> properties = new LinkedList<>();
        try {
            JCRNodeWrapper translatedNode = NodeHelper.getNodeInLanguage(this.node, language, useFallbackLanguage);
            if (names != null) {
                for (String name : names) {
                    if (translatedNode.hasProperty(name)) {
                        properties.add(new GqlJcrProperty(translatedNode.getProperty(name), this));
                    }
                }
            } else {
                for (PropertyIterator it = translatedNode.getProperties(); it.hasNext(); ) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) it.nextProperty();
                    properties.add(new GqlJcrProperty(property, this));
                }
            }

            // There could be a case when a property exists directly on the node and in translation child, we keep translated property only
            if (language != null) {
                String translationPath = "/j:translation_" + language + "/";
                properties.removeIf(p -> {
                    try {
                        return !p.getProperty().getRealProperty().getPath().contains(translationPath)
                                && properties.stream().anyMatch(p2 -> {
                            try {
                                return p.getName().equals(p2.getName()) && p2.getProperty().getRealProperty().getPath().contains(translationPath);
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return FilterHelper.filterList(properties, fieldFilter, environment);
    }

    @Override
    @GraphQLName("property")
    @GraphQLDescription("The GraphQL representation of the property in the requested language; null if the property does not exist")
    public GqlJcrProperty getProperty(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                                      @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language,
                                      @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("useFallbackLanguage") @GraphQLDescription("When set to true, returns the node in the default language if there is no translation for the requested language. Returns null if the option \"Replace untranslated content with the default language content\" is not activated for the site of the requested node. Will also return null if there is no translation for the default language.") Boolean useFallbackLanguage) {
        try {
            JCRNodeWrapper translatedNode = NodeHelper.getNodeInLanguage(this.node, language, useFallbackLanguage);
            if (!translatedNode.hasProperty(name)) {
                return null;
            }
            return new GqlJcrProperty(translatedNode.getProperty(name), this);
        } catch (ItemNotFoundException | PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("children")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("GraphQL representations of the child nodes, according to parameters passed")
    public DXPaginatedData<GqlJcrNode> getChildren(@GraphQLName("names") @GraphQLDescription("Filter of child nodes by their names; null to avoid such filtering") Collection<String> names,
                                                   @GraphQLName("validInLanguage") @GraphQLDescription("Language to use to get children") String validInLanguage,
                                                   @GraphQLName("typesFilter") @GraphQLDescription("Filter of child nodes by their types; null to avoid such filtering") NodeTypesInput typesFilter,
                                                   @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of child nodes by their property values; null to avoid such filtering") NodePropertiesInput propertiesFilter,
                                                   @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                   @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                   @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                   @GraphQLName("includesSelf") @GraphQLDescription("Include the current node itself in results") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean includesSelf,
                                                   DataFetchingEnvironment environment) {
        try {
            return NodeHelper.getPaginatedNodesList(NodeHelper.getNodeInLanguage(node, validInLanguage).getNodes(), names, typesFilter, propertiesFilter, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (ItemNotFoundException e) {
            throw new DataFetchingException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("descendant")
    @GraphQLDescription("GraphQL representation of a descendant node, based on its relative path")
    public GqlJcrNode getDescendant(@GraphQLName("relPath") @GraphQLDescription("Name or relative path of the sub node") @GraphQLNonNull String relPath) {
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
    @GraphQLName("descendants")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("GraphQL representations of the descendant nodes, according to parameters passed")
    public DXPaginatedData<GqlJcrNode> getDescendants(@GraphQLName("typesFilter") @GraphQLDescription("Filter of descendant nodes by their types; null to avoid such filtering") NodeTypesInput typesFilter,
                                                      @GraphQLName("validInLanguage") @GraphQLDescription("Language to use to get children") String validInLanguage,
                                                      @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of descendant nodes by their property values; null to avoid such filtering") NodePropertiesInput propertiesFilter,
                                                      @GraphQLName("recursionTypesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their types; null to avoid such filtering") NodeTypesInput recursionTypesFilter,
                                                      @GraphQLName("recursionPropertiesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their property values; null to avoid such filtering") NodePropertiesInput recursionPropertiesFilter,
                                                      @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                      @GraphQLName("maxDepth") @GraphQLDescription("Maximum depth in JCR tree for descendants from the current node, 0 (or less) for all sub nodes, 1 for one sub level, etc") Integer maxDepth,
                                                      @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                      @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                      DataFetchingEnvironment environment) {
        try {
            // Compute maxDepth relative to the current node depth
            Integer maxDepthValue = maxDepth != null && maxDepth > 0 ? node.getDepth() + maxDepth : null;
            JCRDescendantsNodeIterator it = new JCRDescendantsNodeIterator(NodeHelper.getNodeInLanguage(node, validInLanguage), NodeHelper.getNodesPredicate(null, recursionTypesFilter, recursionPropertiesFilter, maxDepthValue, environment));
            return NodeHelper.getPaginatedNodesList(it, null, typesFilter, propertiesFilter, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (ItemNotFoundException e) {
            throw new DataFetchingException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLName("ancestors")
    @GraphQLDescription("GraphQL representations of the ancestor nodes of the JCR node, top down direction")
    @GraphQLNonNull
    public List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") @GraphQLDescription("The path of the topmost ancestor node to include in the result; null or empty string to include all the ancestor nodes") String upToPath,
                                         @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
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
    @GraphQLName("references")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLNonNull
    @GraphQLDescription("GraphQL representations of the reference properties that target the current JCR Node")
    public DXPaginatedData<GqlJcrProperty> getReferences(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                         @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                         DataFetchingEnvironment environment) {
        List<GqlJcrProperty> references = new LinkedList<GqlJcrProperty>();
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        try {
            collectReferences(node.getWeakReferences(), references, environment);
        } catch (UnsupportedRepositoryOperationException | ConstraintViolationException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        try {
            collectReferences(node.getReferences(), references, environment);
        } catch (RepositoryException e) {
            //EDP implementation will throw UnsupportedRepositoryOperationException on access to getReferences
            if (!(e instanceof UnsupportedRepositoryOperationException)) {
                throw new RuntimeException(e);
            }
        }

        List<GqlJcrProperty> result = FilterHelper.filterConnection(references, fieldFilter, environment);
        if (fieldSorter != null) {
            result.sort(SorterHelper.getFieldComparator(fieldSorter, FieldEvaluator.forConnection(environment)));
        }

        return PaginationHelper.paginate(result, p -> PaginationHelper.encodeCursor(p.getNode().getUuid() + "/" + p.getName()), arguments);
    }

    @Override
    @GraphQLField
    @GraphQLName("referenceCount")
    @GraphQLDescription("Returns count of all references of the node across all sites")
    public Integer getReferenceCount(@GraphQLName("typesFilter") @GraphQLDescription("Filter out referencing types which should not be counted") NodeTypesInput typesFilter) {
        try {
            return jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Integer>() {
                @Override
                public Integer doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                    JCRNodeWrapper nodeWrapper = jcrSessionWrapper.getNode(node.getPath());
                    return countReferences(nodeWrapper.getWeakReferences(), typesFilter) + countReferences(nodeWrapper.getReferences(), typesFilter);
                }
            });
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private int countReferences(PropertyIterator references, NodeTypesInput typesFilter) throws RepositoryException {
        Predicate<JCRNodeWrapper> typesPredicate = getTypesPredicate(typesFilter);

        int count = 0;
        while (references.hasNext()) {
            JCRPropertyWrapper reference = (JCRPropertyWrapper) references.nextProperty();
            JCRNodeWrapper referencingNode = reference.getParent();
            if (referencingNode.isNodeType("jnt:translation") || referencingNode.isNodeType("jnt:referenceInField")) {
                referencingNode = referencingNode.getParent();
            }

            if (typesPredicate.test(referencingNode)) {
                count++;
            }
        }

        return count;
    }

    private void collectReferences(PropertyIterator references, Collection<GqlJcrProperty> gqlReferences, DataFetchingEnvironment environment) throws RepositoryException {
        while (references.hasNext()) {
            JCRPropertyWrapper reference = (JCRPropertyWrapper) references.nextProperty();
            JCRNodeWrapper referencingNode = reference.getParent();
            if (PermissionHelper.hasPermission(referencingNode, environment)) {
                String name = reference.getName();
                String locale = reference.getLocale();
                if (referencingNode.isNodeType("jnt:translation")) {
                    referencingNode = referencingNode.getParent();
                } else if (referencingNode.isNodeType("jnt:referenceInField")) {
                    String s = referencingNode.getProperty("j:fieldName").getValue().getString();
                    referencingNode = referencingNode.getParent();
                    NodeIterator it = referencingNode.getI18Ns();
                    while (it.hasNext()) {
                        Node langNode = it.nextNode();
                        String lang = langNode.getProperty("jcr:language").getString();
                        if (s.endsWith("_" + lang)) {
                            locale = lang;
                            name = StringUtils.substringBeforeLast(s, "_" + lang);
                            break;
                        }
                    }
                }
                GqlJcrNode gqlReferencingNode = SpecializedTypesHandler.getNode(referencingNode);
                GqlJcrProperty gqlReference = gqlReferencingNode.getProperty(name, locale, false);
                if (gqlReference != null) {
                    gqlReferences.add(gqlReference);
                }
            }
        }
    }

    @Override
    @GraphQLName("nodeInWorkspace")
    @GraphQLDescription("GraphQL representation of this node in certain workspace")
    public GqlJcrNode getNodeInWorkspace(@GraphQLName("workspace") @GraphQLDescription("The target workspace") @GraphQLNonNull NodeQueryExtensions.Workspace workspace) {
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
    @GraphQLDescription("Check if the current user has a specific permission")
    public boolean hasPermission(@GraphQLName("permissionName") @GraphQLDescription("The name of the permission") @GraphQLNonNull String permissionName) {
        // first we check if the permission exists to avoid logging an exception just for this (this is done by the JCRNodeWrapperImpl and underlying classes).
        try {
            // we don't need the result as we just want to check if the name exists. If it doesn't an AccessControlException is raised.
            node.getAccessControlManager().privilegeFromName(permissionName);
            return node.hasPermission(permissionName);
        } catch (AccessControlException ace) {
            // in this case the permission name just doesn't exist, we just return false
            return false;
        } catch (RepositoryException re) {
            // in the case of another exception we raised it further.
            throw new RuntimeException(re);
        }
    }

    @Override
    @GraphQLName("aggregatedLastModifiedDate")
    @GraphQLDescription("Get the last modified date of this node and its descendants. The recursion in descendants can be controlled by recursionTypesFilter. If no filter is passed, recursion will stop by default on sub pages.")
    public String getAggregatedLastModifiedDate(@GraphQLName("language") @GraphQLDescription("The language") String language,
                                                @GraphQLName("recursionTypesFilter") @GraphQLDescription("Stop recursion on graphql field values") NodeTypesInput recursionTypesFilter,
                                                DataFetchingEnvironment environment) {
        try {
            JCRNodeWrapper i18node = NodeHelper.getNodeInLanguage(node, language);

            if (recursionTypesFilter == null) {
                // Default, do not recurse on sub pages
                recursionTypesFilter = new NodeTypesInput(MulticriteriaEvaluation.NONE, Collections.singleton(Constants.JAHIANT_PAGE));
            }

            Predicate<JCRNodeWrapper> predicate = getTypesPredicate(recursionTypesFilter);
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
    @GraphQLName("languagesToTranslate")
    @GraphQLDescription("Check if the given locales need translation, by comparing last modifications dates with already existing translations")
    public List<String> getLanguagesToTranslate(@GraphQLName("languagesTranslated") @GraphQLDescription("The translated languages") List<String> languagesTranslated,
                                                @GraphQLName("languagesToCheck") @GraphQLDescription("The languages to check") List<String> languagesToCheck) {

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


    @Override
    @GraphQLName("translationLanguages")
    @GraphQLDescription("Returns languages of available translations for this node")
    public List<String> getTranslationLanguages(
            @GraphQLName("isActiveOnly") @GraphQLDescription("Optional: Return languages only if it is active for the site") Boolean isActiveOnly
    ) {
        List<String> translations = new ArrayList<>();
        try {
            NodeIterator it = node.getI18Ns();
            while (it.hasNext()) {
                Node langNode = it.nextNode();
                String lang = langNode.getProperty("jcr:language").getString();
                translations.add(lang);
            }

            if (isActiveOnly != null && isActiveOnly) {
                JCRSiteNode site = node.getResolveSite();
                Set<String> inactiveLangs = site.getInactiveLanguages();
                translations = translations.stream()
                        .filter(t -> !inactiveLangs.contains(t))
                        .collect(Collectors.toList());
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        return translations;
    }


    @Override
    @GraphQLDescription("Get information on the operations that can be done on this node")
    public GqlOperationsSupport getOperationsSupport() {
        return new GqlOperationsSupport(this);
    }

    @Override
    @GraphQLDescription("Get ACL info for this node")
    public GqlAcl getAcl() {
        return new GqlAcl(node);
    }

    @Override
    @GraphQLDescription("Get node URL")
    public String getUrl() {
        return node.getUrl();
    }

    @Override
    @GraphQLDescription("Get node thumbnail URL")
    public String getThumbnailUrl(@GraphQLName("name") @GraphQLDescription("Thumbnail name") String name, @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("checkIfExists") @GraphQLDescription("Optional: Checks if requested thumbnail node exists, returns null if it doesn't") Boolean checkIfExists) {
        try {
            if (checkIfExists && !node.hasNode(name)) {
                return null;
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }

        return node.getThumbnailUrl(name);
    }

    @Override
    @GraphQLDescription("Get render URL.")
    public String getRenderUrl(@GraphQLName("workspace") @GraphQLDescription("The target workspace") @GraphQLNonNull NodeQueryExtensions.Workspace workspace,
                               @GraphQLName("language") @GraphQLDescription("The language content is rendered in") @GraphQLNonNull String language,
                               @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("findDisplayable") @GraphQLDescription("Finds displayable node") Boolean findDisplayable,
                               DataFetchingEnvironment environment) {
        try {
            String url = getNodeURL(this.node, workspace.getValue(), LanguageCodeConverters.languageCodeToLocale(language), findDisplayable);
            HttpServletResponse httpServletResponse = ContextUtil.getHttpServletResponse(environment.getGraphQlContext());
            HttpServletRequest httpServletRequest = ContextUtil.getHttpServletRequest(environment.getGraphQlContext());
            return urlRewriteService.rewriteOutbound(url, ServletUtil.unwrapRequest(httpServletRequest), httpServletResponse);
        } catch (RepositoryException | ServletException | IOException | InvocationTargetException e) {
            throw new DataFetchingException(e);
        }
    }

    @Override
    @GraphQLName("usages")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLNonNull
    @GraphQLDescription("GraphQL representations of the reference properties that target the current JCR Node")
    public DXPaginatedData<GqlUsage> getUsages(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                               @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                               DataFetchingEnvironment environment) {
        List<GqlUsage> references = new LinkedList<>();
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        try {
            collectUsages(node.getWeakReferences(), references, environment);
        } catch (UnsupportedRepositoryOperationException | ConstraintViolationException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        try {
            collectUsages(node.getReferences(), references, environment);
        } catch (RepositoryException e) {
            //EDP implementation will throw UnsupportedRepositoryOperationException on access to getReferences
            if (!(e instanceof UnsupportedRepositoryOperationException)) {
                throw new RuntimeException(e);
            }
        }

        List<GqlUsage> result = FilterHelper.filterConnection(references, fieldFilter, environment);
        if (fieldSorter != null) {
            result.sort(SorterHelper.getFieldComparator(fieldSorter, FieldEvaluator.forConnection(environment)));
        }

        return PaginationHelper.paginate(result, u -> PaginationHelper.encodeCursor(u.getNode().getUuid()), arguments);
    }


    @Override
    @GraphQLNonNull
    @GraphQLName("isExternal")
    @GraphQLDescription("true if node is under a mounted node")
    public boolean isExternal() {
        return !node.getProvider().isDefault();
    }

    private void collectUsages(PropertyIterator references, Collection<GqlUsage> gqlReferences, DataFetchingEnvironment environment) throws RepositoryException {
        while (references.hasNext()) {
            JCRPropertyWrapper reference = (JCRPropertyWrapper) references.nextProperty();
            JCRNodeWrapper referencingNode = reference.getParent();
            if (PermissionHelper.hasPermission(referencingNode, environment)) {
                String name = reference.getName();
                String locale = reference.getLocale();
                if (referencingNode.isNodeType("jnt:translation")) {
                    referencingNode = referencingNode.getParent();
                } else if (referencingNode.isNodeType("jnt:referenceInField")) {
                    String s = referencingNode.getProperty("j:fieldName").getValue().getString();
                    referencingNode = referencingNode.getParent();
                    NodeIterator it = referencingNode.getI18Ns();
                    if (!it.hasNext()) {
                        name = s;
                    } else {
                        while (it.hasNext()) {
                            Node langNode = it.nextNode();
                            String lang = langNode.getProperty("jcr:language").getString();
                            if (s.endsWith("_" + lang)) {
                                locale = lang;
                                name = StringUtils.substringBeforeLast(s, "_" + lang);
                                break;
                            }
                        }
                    }
                }
                GqlJcrNode gqlReferencingNode = SpecializedTypesHandler.getNode(referencingNode);
                GqlJcrProperty gqlReference = gqlReferencingNode.getProperty(name, locale, false);
                gqlReferences.stream()
                        .filter(gqlUsage -> gqlUsage.getNode().getUuid().equals(gqlReferencingNode.getUuid()))
                        .findFirst()
                        .orElseGet(() -> {
                            GqlUsage usage = new GqlUsage(gqlReferencingNode);
                            gqlReferences.add(usage);
                            return usage;
                        }).addUsage(gqlReference);
            }
        }
    }

    // This code was borrowed from gwt related package in the core: https://github.com/Jahia/jahia-private/blob/0ddbc6f48459751a4e7a1729c234f28c764d12ea/core/src/main/java/org/jahia/ajax/gwt/helper/NodeHelper.java#L102-L139
    // TODO: A TECH ticket to move this code was created: TECH-1875
    private String getNodeURL(JCRNodeWrapper node, final String workspace, final Locale locale, boolean findDisplayable)
            throws RepositoryException {
        String servlet = "render";
        String url = Jahia.getContextPath() + "/cms/" + servlet + "/" + workspace + "/" + locale;

        Resource resource = new Resource(node, "html", null, Resource.CONFIGURATION_PAGE);
        RenderContext renderContext = new RenderContext(null, null, node.getSession().getUser());
        renderContext.setMainResource(resource);
        renderContext.setServletPath("/cms/" + servlet);
        JCRNodeWrapper nodeForURL = node;
        if (findDisplayable) {
            nodeForURL = JCRContentUtils.findDisplayableNode(node, renderContext);
            if (nodeForURL != null && !nodeForURL.getIdentifier().equals(node.getIdentifier())) {
                resource = new Resource(nodeForURL, "html", null, Resource.CONFIGURATION_PAGE);
            }
        }
        Template template = RenderService.getInstance().resolveTemplate(resource, renderContext);
        String extensionName;
        if (template != null || nodeForURL.isNodeType("jnt:page") || nodeForURL.isNodeType("jmix:mainResource")) {
            extensionName = ".html";
        } else {
            extensionName = ".content-template.html";
        }

        url += nodeForURL.getPath() + extensionName;

        return url;
    }
}
