package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.services.content.JCRNodeWrapper;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import graphql.annotations.GraphQLTypeResolver;

import java.util.Collection;
import java.util.List;

/**
 * GraphQL representation of a JCR node.
 */
@GraphQLName("JCRNode")
@GraphQLTypeResolver(SpecializedTypesHandler.NodeTypeResolver.class)
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
    @GraphQLNonNull
    String getUuid();

    /**
     * @return The name of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLNonNull
    String getName();

    /**
     * @return The path of the JCR node this object represents
     */
    @GraphQLField
    @GraphQLNonNull
    String getPath();

    /**
     * @param language The language to obtain the display name in
     * @return The display name of the JCR node this object represents in the requested language
     */
    @GraphQLField
    String getDisplayName(@GraphQLName("language") String language);

    /**
     * @return GraphQL representation of the parent JCR node
     */
    @GraphQLField
    GqlJcrNode getParent();

    /**
     * Get GraphQL representations of multiple of properties of the JCR node.
     *
     * @param names The names of the JCR properties; null or empty collection to obtain all properties
     * @param language The language to obtain the properties in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones
     * @return GraphQL representations of the properties, in either the requested or the default language
     */
    @GraphQLField
    @GraphQLNonNull
    Collection<GqlJcrProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                             @GraphQLName("language") String language);

    /**
     * Get a GraphQL representation of a single property of the JCR node.
     *
     * @param name The name of the JCR property
     * @param language The language to obtain the property in; must be a valid language code in case any internationalized properties are requested, does not matter for non-internationalized ones
     * @return The GraphQL representation of the property, in either the requested or the default language
     */
    @GraphQLField
    GqlJcrProperty getProperty(@GraphQLName("name") String name,
                               @GraphQLName("language") String language);

    /**
     * Get GraphQL representations of child nodes of the JCR node, according to filters specified. A child node must pass through all non-null filters in order to be included in the result.
     *
     * @param names Filter of child nodes by their names; null to avoid such filtering
     * @param typesFilter Filter of child nodes by their types; null to avoid such filtering
     * @param propertiesFilter Filter of child nodes by their property values; null to avoid such filtering
     * @return GraphQL representations of the child nodes, according to parameters passed
     */
    @GraphQLField
    @GraphQLNonNull
    List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                 @GraphQLName("typesFilter") NodeTypesInput typesFilter,
                                 @GraphQLName("propertiesFilter") NodePropertiesInput propertiesFilter);

    /**
     * Get GraphQL representations of the ancestor nodes of the JCR node.
     *
     * @param upToPath The path of the topmost ancestor node to include in the result; null or empty string to include all the ancestor nodes
     * @return GraphQL representations of the ancestor nodes of the JCR node, top down direction
     */
    @GraphQLField
    @GraphQLNonNull
    List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath);

    /**
     * @return GraphQL representation of the site the JCR node belongs to, or the system site in case the node does not belong to any site
     */
    @GraphQLField
    @GraphQLNonNull
    GqlJcrSite getSite();

    /**
     * Get GraphQL representation of the JCR node as a mixin type it has.
     *
     * @param type The mixin type name
     * @return GraphQL representation of the JCR node as the mixin type, or null in case the node does not actually has the mixin type
     */
    @GraphQLField
    GqlJcrNode asMixin(@GraphQLName("type") String type);

    /**
     * A way to evaluate a criteria consisting of multiple sub-criteria.
     */
    enum MulticriteriaEvaluation {

        /**
         * The result criteria evaluates positive iff all sub-criteria evaluate positive.
         */
        ALL,

        /**
         * The result criteria evaluates positive if any sub-criteria evaluates positive.
         */
        ANY
    }

    /**
     * Nodes filter based on their types.
     */
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
        public MulticriteriaEvaluation getMulticriteriaEvaluation() {
            return multicriteriaEvaluation;
        }

        /**
         * @return Node type names required for a node to pass the filter
         */
        @GraphQLField
        @GraphQLNonNull
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
        PRESENT,

        /**
         * The property is absent.
         */
        ABSENT,

        /**
         * The property value is equal to given one.
         */
        EQUAL,

        /**
         * The property value is different from given one.
         */
        DIFFERENT
    }

    /**
     * Nodes filter based on their properties.
     */
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
        public MulticriteriaEvaluation getMulticriteriaEvaluation() {
            return multicriteriaEvaluation;
        }

        /**
         * @return Individual property filters
         */
        @GraphQLField
        @GraphQLName("filters")
        @GraphQLNonNull
        public Collection<NodePropertyInput> getPropertyFilters() {
            return propertyFilters;
        }
    }

    /**
     * Nodes filter based on a single property.
     */
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
        public String getLanguage() {
            return language;
        }

        /**
         * @return The way to evaluate the property; null indicates default (EQUAL)
         */
        @GraphQLField
        @GraphQLName("evaluation")
        public PropertyEvaluation getPropertyEvaluation() {
            return propertyEvaluation;
        }

        /**
         * @return The name of the property to filter by
         */
        @GraphQLField
        @GraphQLName("property")
        @GraphQLNonNull
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * @return The value to evaluate the property against
         */
        @GraphQLField
        @GraphQLName("value")
        public String getPropertyValue() {
            return propertyValue;
        }
    }
}
