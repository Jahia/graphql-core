package org.jahia.modules.graphql.provider.dxm.osgiconfig;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.modulemanager.util.PropertiesList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@GraphQLName("ConfigurationItemsListQuery")
public class GqlListQuery {
    PropertiesList propertiesList;

    public GqlListQuery(PropertiesList propertiesList) {
        this.propertiesList = propertiesList;
    }

    @GraphQLField
    @GraphQLDescription("Adds a new structured object to the list")
    public int getSize() {
        return propertiesList.getSize();
    }

    @GraphQLField
    @GraphQLDescription("Get sub structured object values")
    public List<GqlValueQuery> getObjects() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getValues).map(GqlValueQuery::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get sub lists of items")
    public List<GqlListQuery> getLists() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getList).map(GqlListQuery::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get property values")
    public List<String> getValues() {
        return IntStream.range(0, propertiesList.getSize()).boxed()
                .map(propertiesList::getProperty)
                .collect(Collectors.toList());
    }
}
