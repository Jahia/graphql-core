package org.jahia.modules.graphql.provider.dxm.osgiconfig;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.modulemanager.util.PropertiesList;

@GraphQLName("ConfigurationItemsListMutation")
@GraphQLDescription("Mutation for configuration list of values")
public class GqlListMutation {
    PropertiesList propertiesList;

    public GqlListMutation(PropertiesList propertiesList) {
        this.propertiesList = propertiesList;
    }

    @GraphQLField
    @GraphQLDescription("Adds a new structured object to the list")
    public GqlValueMutation addObject() {
        return new GqlValueMutation(propertiesList.addValues());
    }

    @GraphQLField
    @GraphQLDescription("Adds a new sub list to the list")
    public GqlListMutation addList() {
        return new GqlListMutation(propertiesList.addList());
    }

    @GraphQLField
    @GraphQLDescription("Adds a property value to the list")
    public String addValue(@GraphQLName("value") String value) {
        propertiesList.addProperty(value);
        return value;
    }
}
