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
     * Get GraphQL representations of child nodes of the JCR node.
     *
     * @param names The names of the child nodes; null or empty collection to obtain all children
     * @param anyType Type names (either immediate or super) of child nodes to include in the result; null or empty collection to include children of any types
     * @param properties Property names/value pairs child nodes must have to be included in the result; null or empty collection to avoid filtering children by their properties
     * @return GraphQL representations of the child nodes, according to parameters passed
     */
    @GraphQLField
    @GraphQLNonNull
    List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                 @GraphQLName("anyType") Collection<String> anyType,
                                 @GraphQLName("properties") Collection<PropertyFilterInput> properties);

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
     * Property name/value pair used to filter JCR nodes by their properties.
     */
    public static class PropertyFilterInput {

        private String name;
        private String value;

        /**
         * Create a JCR property name/value pair.
         *
         * @param name The name of the property
         * @param value The value of the property as a String
         */
        public PropertyFilterInput(@GraphQLName("name") String name, @GraphQLName("value") String value) {
            this.name = name;
            this.value = value;
        }

        @GraphQLField
        public String getName() {
            return name;
        }

        @GraphQLField
        public String getValue() {
            return value;
        }
    }
}
