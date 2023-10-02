package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.annotationTypes.*;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("Usage")
@GraphQLDescription("GraphQL representation of a usage (node holding the reference + list of properties referencing the caller)")
public class GqlUsage {

    private final GqlJcrNode node;

    List<GqlJcrProperty> usages;

    /**
     * @return The GraphQL representation of the JCR node the property belongs to.
     */
    @GraphQLField
    @GraphQLName("node")
    @GraphQLNonNull
    @GraphQLDescription("The GraphQL representation of the JCR node the property belongs to.")
    public GqlJcrNode getNode() {
        return node;
    }

    public GqlUsage(GqlJcrNode node) {
        this.node = node;
        this.usages = new ArrayList<>();
    }

    public void addUsage(GqlJcrProperty property) {
        usages.add(property);
    }

    /**
     * @return The GraphQL representation of the JCR node the property belongs to.
     */
    @GraphQLField
    @GraphQLName("usages")
    @GraphQLNonNull
    @GraphQLDescription("The GraphQL representation of the references on this node.")
    public List<GqlJcrProperty> getUsages() {
        return usages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GqlUsage gqlUsage = (GqlUsage) o;

        return node.getUuid().equals(gqlUsage.node.getUuid());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(node).toHashCode();
    }
}
