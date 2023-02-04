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
import org.jahia.modules.graphql.provider.dxm.acl.GqlAcl;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldGroupingInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.predicate.MulticriteriaEvaluation;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.Collection;
import java.util.List;

/**
 * GraphQL representation of a JCR node.
 */
@GraphQLName("JCRNode")
@GraphQLTypeResolver(SpecializedTypesHandler.NodeTypeResolver.class)
@GraphQLDescription("GraphQL representation of a JCR node")
public interface GqlJcrNode {

    /**
     * @return The actual JCR node this object represents
     */
    JCRNodeWrapper getNode();

    /**
     * @return The type of the JCR node this object represents
     */
    String getType();

    /**
     * @return The UUID of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLName("uuid")
    @GraphQLNonNull
    @GraphQLDescription("The UUID of the JCR node this object represents")
    String getUuid();

    /**
     * @return Get the workspace of the query
     */
    @GraphQLField
    @GraphQLName("workspace")
    @GraphQLNonNull
    @GraphQLDescription("Get the workspace of the query")
    NodeQueryExtensions.Workspace getWorkspace();

    /**
     * @return The name of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("The name of the JCR node this object represents")
    String getName();

    /**
     * @return The path of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLName("path")
    @GraphQLNonNull
    @GraphQLDescription("The path of the JCR node this object represents")
    String getPath();

    /**
     * @param language The language to obtain the display name in
     * @return The display name of the JCR node this object represents in the requested language
     */
    @GraphQLField
    @GraphQLName("displayName")
    @GraphQLDescription("The display name of the JCR node this object represents in the requested language")
    String getDisplayName(@GraphQLName("language") @GraphQLDescription("The language to obtain the display name in") String language);

    /**
     * @return GraphQL representation of the parent JCR node
     */
    @GraphQLField
    @GraphQLName("parent")
    @GraphQLDescription("GraphQL representation of the parent JCR node")
    GqlJcrNode getParent();

    /**
     * Get GraphQL representations of multiple of properties of the JCR node.
     *
     * @param names The names of the JCR properties; null to obtain all properties
     * @param language The language to obtain the properties in; must be a valid language code in case any internationalized properties are requested, does not matter for non-internationalized ones
     * @param useFallbackLanguage if true and that the site of the node have a default locale, this locale will be used to get the translated
     *                          properties of the node when there is no translation for the asked language
     * @return GraphQL representations of the properties in the requested language
     */
    @GraphQLField
    @GraphQLName("properties")
    @GraphQLNonNull
    @GraphQLDescription("GraphQL representations of the properties in the requested language")
    Collection<GqlJcrProperty> getProperties(
            @GraphQLName("names") @GraphQLDescription("The names of the JCR properties; null to obtain all properties") Collection<String> names,
            @GraphQLName("language") @GraphQLDescription("The language to obtain the properties in; must be a valid language code in case any internationalized properties are requested, does not matter for non-internationalized ones") String language,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
            @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("useFallbackLanguage") @GraphQLDescription("When set to true, returns the node in the default language if there is no translation for the requested language. Returns null if the option \"Replace untranslated content with the default language content\" is not activated for the site of the requested node. Will also return null if there is no translation for the default language.") Boolean useFallbackLanguage,
            DataFetchingEnvironment environment);

    /**
     * Get a GraphQL representation of a single property of the JCR node.
     *
     * @param name The name of the JCR property
     * @param language The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones
     * @param useFallbackLanguage if true and that the site of the node have a default locale, this locale will be used to get the translated
     *                          property of the node when there is no translation for the asked language
     * @return The GraphQL representation of the property in the requested language; null if the property does not exist
     */
    @GraphQLField
    @GraphQLName("property")
    @GraphQLDescription("The GraphQL representation of the property in the requested language; null if the property does not exist")
    GqlJcrProperty getProperty(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
            @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid "
                    + "language code for internationalized properties, does not matter for non-internationalized ones") String language,
            @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLName("useFallbackLanguage") @GraphQLDescription("When set to true, returns the node in the default language if there is no translation for the requested language. Returns null if the option \"Replace untranslated content with the default language content\" is not activated for the site of the requested node. Will also return null if there is no translation for the default language.") Boolean useFallbackLanguage);

    /**
     * Get GraphQL representations of child nodes of the JCR node, according to filters specified. A child node must pass through all non-null filters in order to be included in the result.
     *
     * @param names Filter of child nodes by their names; null to avoid such filtering
     * @param typesFilter Filter of child nodes by their types; null to avoid such filtering
     * @param propertiesFilter Filter of child nodes by their property values; null to avoid such filtering
     * @param environment the execution content instance
     * @return GraphQL representations of the child nodes, according to parameters passed
     * @throws GqlJcrWrongInputException In case any of the property filters passed as a part of the propertiesFilter is inconsistent (for example missing a property value to be used for comparison)
     */
    @GraphQLField
    @GraphQLName("children")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("GraphQL representations of the child nodes, according to parameters passed")
    DXPaginatedData<GqlJcrNode> getChildren(@GraphQLName("names") @GraphQLDescription("Filter of child nodes by their names; null to avoid such filtering") Collection<String> names,
                                            @GraphQLName("validInLanguage") @GraphQLDescription("Language to use to get children") String validInLanguage,
                                            @GraphQLName("typesFilter") @GraphQLDescription("Filter of child nodes by their types; null to avoid such filtering") NodeTypesInput typesFilter,
                                            @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of child nodes by their property values; null to avoid such filtering") NodePropertiesInput propertiesFilter,
                                            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values")  FieldFiltersInput fieldFilter,
                                            @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                            @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                            @GraphQLName("includesSelf") @GraphQLDescription("Include the current node itself in results") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean includesSelf,
                                            DataFetchingEnvironment environment)
    throws GqlJcrWrongInputException;

    /**
     * Get GraphQL representation of a descendant node, based on relative path.
     *
     * @param relPath Name or relative path of the sub node
     * @return GraphQL representation of the descendant node; null in case no descendant node exists at the specified path
     * @throws GqlJcrWrongInputException In case of malformed relative descendant node path
     */
    @GraphQLField
    @GraphQLName("descendant")
    @GraphQLDescription("GraphQL representation of a descendant node, based on its relative path")
    GqlJcrNode getDescendant(@GraphQLName("relPath") @GraphQLDescription("Name or relative path of the sub node") @GraphQLNonNull String relPath)
    throws GqlJcrWrongInputException;

    /**
     * Get GraphQL representations of descendant nodes of the JCR node, according to filters specified. A descendant node must pass through all non-null filters in order to be included in the result.
     *
     * @param typesFilter Filter of descendant nodes by their types; null to avoid such filtering
     * @param propertiesFilter Filter of descendant nodes by their property values; null to avoid such filtering
     * @param environment the execution content instance
     * @return GraphQL representations of the descendant nodes, according to parameters passed
     * @throws GqlJcrWrongInputException In case any of the property filters passed as a part of the propertiesFilter is inconsistent (for example missing a property value to be used for comparison)
     */
    @GraphQLField
    @GraphQLName("descendants")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("GraphQL representations of the descendant nodes, according to parameters passed")
    DXPaginatedData<GqlJcrNode> getDescendants(@GraphQLName("typesFilter") @GraphQLDescription("Filter of descendant nodes by their types; null to avoid such filtering") NodeTypesInput typesFilter,
                                               @GraphQLName("validInLanguage") @GraphQLDescription("Language to use to get children") String validInLanguage,
                                               @GraphQLName("propertiesFilter") @GraphQLDescription("Filter of descendant nodes by their property values; null to avoid such filtering") NodePropertiesInput propertiesFilter,
                                               @GraphQLName("recursionTypesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their types; null to avoid such filtering") NodeTypesInput recursionTypesFilter,
                                               @GraphQLName("recursionPropertiesFilter") @GraphQLDescription("Filter out and stop recursion on nodes by their property values; null to avoid such filtering") NodePropertiesInput recursionPropertiesFilter,
                                               @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                               @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                               @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                               DataFetchingEnvironment environment)
    throws GqlJcrWrongInputException;

    /**
     * Get GraphQL representations of the ancestor nodes of the JCR node.
     *
     * @param upToPath The path of the topmost ancestor node to include in the result; null or empty string to include all the ancestor nodes
     * @return GraphQL representations of the ancestor nodes of the JCR node, top down direction
     * @throws GqlJcrWrongInputException In case the upToPath parameter value is not a valid path of an ancestor node of this node
     */
    @GraphQLField
    @GraphQLName("ancestors")
    @GraphQLNonNull
    @GraphQLDescription("GraphQL representations of the ancestor nodes of the JCR node, top down direction")
    List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") @GraphQLDescription("The path of the topmost ancestor node to include in the result; null or empty string to include all the ancestor nodes") String upToPath,
                                  @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                  DataFetchingEnvironment environment)
    throws GqlJcrWrongInputException;

    /**
     * @param environment the execution content instance
     * @return GraphQL representations of the reference properties that target the JCR Node
     */
    @GraphQLField
    @GraphQLName("references")
    @GraphQLNonNull
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("GraphQL representations of the reference properties that target the current JCR Node")
    DXPaginatedData<GqlJcrProperty> getReferences(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                  DataFetchingEnvironment environment);

    /**
     * Get GraphQL representation of this node in certain workspace.
     *
     * @param workspace The target workspace
     * @return GraphQL representation of this node in certain workspace; null in case there is no corresponding node in the target workspace
     */
    @GraphQLField
    @GraphQLName("nodeInWorkspace")
    @GraphQLDescription("GraphQL representation of this node in certain workspace")
    GqlJcrNode getNodeInWorkspace(@GraphQLName("workspace") @GraphQLDescription("The target workspace") @GraphQLNonNull NodeQueryExtensions.Workspace workspace);

    /**
     * Check if the current user has a specific permission
     *
     * @param permissionName The permission to check
     * @return true if the permission has the permission, false otherwise
     */
    @GraphQLField
    @GraphQLDescription("Check if the current user has a specific permission")
    boolean hasPermission(@GraphQLName("permissionName") @GraphQLDescription("The name of the permission") @GraphQLNonNull String permissionName);

    /**
     * Get the last modified date of this node and its descendants. The recursion in descendants can be controlled by recursionTypesFilter.
     * If no filter is passed, recursion will stop by default on sub pages.
     *
     * @return true if the permission has the permission, false otherwise
     */
    @GraphQLField
    @GraphQLName("aggregatedLastModifiedDate")
    @GraphQLDescription("Get the last modified date of this node and its descendants. The recursion in descendants can be controlled by recursionTypesFilter. If no filter is passed, recursion will stop by default on sub pages.")
    String getAggregatedLastModifiedDate(@GraphQLName("language") @GraphQLDescription("The language to use to get the last modified date, if not specified, returns last modification date in any language") String language,
                                         @GraphQLName("recursionTypesFilter") @GraphQLDescription("Stop recursion on nodes by their types; null to avoid such filtering") NodeTypesInput recursionTypesFilter,
                                         DataFetchingEnvironment environment);

    /**
     * Check if the given locales need translation, by comparing last modifications dates with already existing translations
     *
     * @return the locales to be translate, if the given locales doesn't exist or the given locales last modification are older than already translated locales
     */
    @GraphQLField
    @GraphQLName("languagesToTranslate")
    @GraphQLDescription("Check if the given locales need translation, by comparing last modifications dates with already existing translations")
    List<String> getLanguagesToTranslate(@GraphQLName("languagesTranslated") @GraphQLDescription("List of known translated languages, will be used to compare modifications dates") List<String> languagesTranslated,
                                   @GraphQLName("languagesToCheck") @GraphQLDescription("List of languages potentially to be translated") List<String> languagesToCheck);

    @GraphQLField
    @GraphQLName("translationLanguages")
    @GraphQLDescription("Returns languages of available translations for this node")
    public List<String> getTranslationLanguages(
            @GraphQLName("isActiveOnly") @GraphQLDescription("Optional: Return languages only if it is active for the site") Boolean isActiveOnly
    );

    /**
     * Get information on the operations that can be done on this node
     *
     * @return
     */
    @GraphQLField
    @GraphQLDescription("Get information on the operations that can be done on this node")
    GqlOperationsSupport getOperationsSupport();

    @GraphQLField
    @GraphQLDescription("Get ACL info for this node")
    public GqlAcl getAcl();

    /**
     * Nodes filter based on their types.
     */
    @GraphQLDescription("Node types selection")
    static class NodeTypesInput {

        private MulticriteriaEvaluation multicriteriaEvaluation;
        private Collection<String> types;

        /**
         * Create a filter instance.
         *
         * @param multicriteriaEvaluation The way to combine multiple type criteria; null to use ANY by default
         * @param types Node type names required for a node to pass the filter
         */
        public NodeTypesInput(@GraphQLName("multi") MulticriteriaEvaluation multicriteriaEvaluation,
                              @GraphQLName("types") @GraphQLNonNull Collection<String> types) {
            this.multicriteriaEvaluation = multicriteriaEvaluation;
            this.types = types;
        }

        /**
         * @return The way to combine multiple type criteria; null indicates default (ANY)
         */
        @GraphQLField
        @GraphQLName("multi")
        @GraphQLDescription("The way to combine multiple type criteria; null indicates default (ANY)")
        public MulticriteriaEvaluation getMulticriteriaEvaluation() {
            return multicriteriaEvaluation;
        }

        /**
         * @return Node type names required for a node to pass the filter
         */
        @GraphQLField
        @GraphQLName("types")
        @GraphQLNonNull
        @GraphQLDescription("Node type names required for a node to pass the filter")
        public Collection<String> getTypes() {
            return types;
        }
    }

    /**
     * The way to evaluate a node property.
     */
    enum PropertyEvaluation {

        /**
         * The property is present.
         */
        @GraphQLDescription("The property is present")
        PRESENT,

        /**
         * The property is absent.
         */
        @GraphQLDescription("The property is absent")
        ABSENT,

        /**
         * The property value is equal to given one.
         */
        @GraphQLDescription("The property value is equal to given one")
        EQUAL,

        /**
         * The property value is different from given one.
         */
        @GraphQLDescription("The property value is different from given one")
        DIFFERENT
    }

    /**
     * Nodes filter based on their properties.
     */
    @GraphQLDescription("Node properties selection")
    static class NodePropertiesInput {

        private MulticriteriaEvaluation multicriteriaEvaluation;
        private Collection<NodePropertyInput> propertyFilters;

        /**
         * Create a filter instance.
         *
         * @param multicriteriaEvaluation The way to combine multiple individual property filters; null to use ALL by default
         * @param propertyFilters Individual property filters
         */
        public NodePropertiesInput(@GraphQLName("multi") MulticriteriaEvaluation multicriteriaEvaluation,
                                   @GraphQLName("filters") @GraphQLNonNull Collection<NodePropertyInput> propertyFilters) {
            this.multicriteriaEvaluation = multicriteriaEvaluation;
            this.propertyFilters = propertyFilters;
        }

        /**
         * @return The way to combine multiple individual property filters; null indicates default (ALL)
         */
        @GraphQLField
        @GraphQLName("multi")
        @GraphQLDescription("The way to combine multiple individual property filters; null indicates default (ALL)")
        public MulticriteriaEvaluation getMulticriteriaEvaluation() {
            return multicriteriaEvaluation;
        }

        /**
         * @return Individual property filters
         */
        @GraphQLField
        @GraphQLName("filters")
        @GraphQLNonNull
        @GraphQLDescription("Individual property filters")
        public Collection<NodePropertyInput> getPropertyFilters() {
            return propertyFilters;
        }
    }

    /**
     * Nodes filter based on a single property.
     */
    @GraphQLDescription("Node property selection")
    static class NodePropertyInput {

        private String language;
        private PropertyEvaluation propertyEvaluation;
        private String propertyName;
        private String propertyValue;

        /**
         * Create a filter instance.
         *
         * @param language Language to use when evaluating the property; must be a valid language code for internationalized properties, does not matter for non-internationalized ones
         * @param propertyEvaluation The way to evaluate the property; null to use EQUAL by default
         * @param propertyName The name of the property to filter by
         * @param propertyValue The value to evaluate the property against; only required for EQUAL and DIFFERENT evaluation, does not matter for PRESENT and ABSENT
         */
        public NodePropertyInput(@GraphQLName("language") String language,
                                 @GraphQLName("evaluation") PropertyEvaluation propertyEvaluation,
                                 @GraphQLName("property") @GraphQLNonNull String propertyName,
                                 @GraphQLName("value") String propertyValue) {
            this.language = language;
            this.propertyEvaluation = propertyEvaluation;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        /**
         * @return Language to use when evaluating the property
         */
        @GraphQLField
        @GraphQLName("language")
        @GraphQLDescription("Language to use when evaluating the property")
        public String getLanguage() {
            return language;
        }

        /**
         * @return The way to evaluate the property; null indicates default (EQUAL)
         */
        @GraphQLField
        @GraphQLName("evaluation")
        @GraphQLDescription("The way to evaluate the property; null indicates default (EQUAL)")
        public PropertyEvaluation getPropertyEvaluation() {
            return propertyEvaluation;
        }

        /**
         * @return The name of the property to filter by
         */
        @GraphQLField
        @GraphQLName("property")
        @GraphQLNonNull
        @GraphQLDescription("The name of the property to filter by")
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * @return The value to evaluate the property against
         */
        @GraphQLField
        @GraphQLName("value")
        @GraphQLDescription("The value to evaluate the property against")
        public String getPropertyValue() {
            return propertyValue;
        }

    }
}
