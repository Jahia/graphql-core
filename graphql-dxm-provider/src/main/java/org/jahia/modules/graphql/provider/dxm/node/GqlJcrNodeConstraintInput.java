package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties.
 */
@GraphQLDescription("An optional part of the JCR node criteria to filter nodes, specifically by their arbitrary properties")
public class GqlJcrNodeConstraintInput {

    private String like;
    private String contains;
    private String property;

    /**
     * Create an instance of the node constraint.
     *
     * Exactly one parameter that defines the way node property values are compared/matched (such as 'like', 'contains', etc) must be non-null.
     *
     * @param like A value to compare the node property value to, using the 'like' operator
     * @param contains A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     * @param property The name of the node property to compare/match; should be null when not applicable, may be null when optional, dependent on other parameter values
     */
    public GqlJcrNodeConstraintInput(
        @GraphQLName("like") @GraphQLDescription("A value to compare the node property value to, using the 'like' operator") String like,
        @GraphQLName("contains") @GraphQLDescription("A search expression to match the node property value(s) against, either specific property only or all node properties, dependent on the 'property' parameter value passed") String contains,
        @GraphQLName("property") @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values") String property
    ) {
        this.like = like;
        this.contains = contains;
        this.property = property;
    }

    /**
     * @return A value to compare the node property value to, using the 'like' operator
     */
    @GraphQLField
    @GraphQLName("like")
    @GraphQLDescription("A value to compare the node property value to, using the 'like' operator")
    public String getLike() {
        return like;
    }

    /**
     * @return A search expression to match the node property value(s) against: dependent on whether the property parameter is null, either that specific property only or all node properties will be matched
     */
    @GraphQLField
    @GraphQLName("contains")
    @GraphQLDescription("A search expression to match the node property value(s) against, either specific property only or all node properties, dependent on the 'property' parameter value passed")
    public String getContains() {
        return contains;
    }

    /**
     * @return The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values
     */
    @GraphQLField
    @GraphQLName("property")
    @GraphQLDescription("The name of the node property to compare/match; may be null when optional or not applicable, dependent on other parameter values")
    public String getProperty() {
        return property;
    }
}