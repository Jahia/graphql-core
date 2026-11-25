package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Collection;

/**
 * The values of a form that the user has not saved, which {@code jcr(unsavedValues:)} shows on their node. The
 * {@link org.jahia.modules.graphql.provider.dxm.versioning.JcrVersionSessions} read the argument when the operation
 * starts, so the field itself never reads this object.
 */
@GraphQLName("JCRUnsavedValues")
@GraphQLDescription("The values of a form that the user has not saved, for one node")
public class GqlJcrUnsavedValuesInput {

    private final String node;
    private final Collection<GqlJcrPropertyInput> properties;
    private final Collection<GqlJcrDeletedPropertyInput> propertiesToDelete;
    private final Collection<String> mixinsToAdd;
    private final Collection<String> mixinsToDelete;

    public GqlJcrUnsavedValuesInput(@GraphQLName("node") @GraphQLNonNull @GraphQLDescription("The path or the UUID of the node of the form") String node,
                                    @GraphQLName("properties") @GraphQLDescription("The properties that the form sets, as setPropertiesBatch takes them") Collection<GqlJcrPropertyInput> properties,
                                    @GraphQLName("propertiesToDelete") @GraphQLDescription("The properties that the form removes, as deletePropertiesBatch takes them") Collection<GqlJcrDeletedPropertyInput> propertiesToDelete,
                                    @GraphQLName("mixinsToAdd") @GraphQLDescription("The mixins that the form adds") Collection<String> mixinsToAdd,
                                    @GraphQLName("mixinsToDelete") @GraphQLDescription("The mixins that the form removes") Collection<String> mixinsToDelete) {
        this.node = node;
        this.properties = properties;
        this.propertiesToDelete = propertiesToDelete;
        this.mixinsToAdd = mixinsToAdd;
        this.mixinsToDelete = mixinsToDelete;
    }

    @GraphQLField
    @GraphQLName("node")
    @GraphQLNonNull
    @GraphQLDescription("The path or the UUID of the node of the form")
    public String getNode() {
        return node;
    }

    @GraphQLField
    @GraphQLName("properties")
    @GraphQLDescription("The properties that the form sets, as setPropertiesBatch takes them")
    public Collection<GqlJcrPropertyInput> getProperties() {
        return properties;
    }

    @GraphQLField
    @GraphQLName("propertiesToDelete")
    @GraphQLDescription("The properties that the form removes, as deletePropertiesBatch takes them")
    public Collection<GqlJcrDeletedPropertyInput> getPropertiesToDelete() {
        return propertiesToDelete;
    }

    @GraphQLField
    @GraphQLName("mixinsToAdd")
    @GraphQLDescription("The mixins that the form adds")
    public Collection<String> getMixinsToAdd() {
        return mixinsToAdd;
    }

    @GraphQLField
    @GraphQLName("mixinsToDelete")
    @GraphQLDescription("The mixins that the form removes")
    public Collection<String> getMixinsToDelete() {
        return mixinsToDelete;
    }
}
