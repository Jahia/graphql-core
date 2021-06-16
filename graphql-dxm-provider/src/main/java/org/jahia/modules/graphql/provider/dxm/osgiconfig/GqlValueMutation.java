package org.jahia.modules.graphql.provider.dxm.osgiconfig;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.modulemanager.util.PropertiesValues;

@GraphQLName("ConfigurationItemValuesMutation")
public class GqlValueMutation {
    protected PropertiesValues propertiesValues;

    public GqlValueMutation(PropertiesValues values) {
        this.propertiesValues = values;
    }

    protected PropertiesValues getPropertiesValues() {
        return propertiesValues;
    }

    @GraphQLField
    @GraphQLDescription("Modify a structured object")
    public GqlValueMutation mutateObject(@GraphQLName("name") String name) {
        return new GqlValueMutation(getPropertiesValues().getValues(name));
    }

    @GraphQLField
    @GraphQLDescription("Modify a list of items")
    public GqlListMutation mutateList(@GraphQLName("name") String name) {
        return new GqlListMutation(getPropertiesValues().getList(name));
    }

    @GraphQLField
    @GraphQLDescription("Set a property value")
    public String setValue(@GraphQLName("name") String name, @GraphQLName("value") String value) {
        getPropertiesValues().setProperty(name, value);
        return value;
    }

    @GraphQLField
    @GraphQLDescription("Remove the specified property and all sub/list properties")
    public boolean remove(@GraphQLName("name") String name) {
        getPropertiesValues().remove(name);
        return true;
    }

}
