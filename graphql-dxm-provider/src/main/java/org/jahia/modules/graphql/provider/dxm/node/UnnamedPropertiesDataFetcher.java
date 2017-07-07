package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;

import javax.jcr.PropertyType;

public class UnnamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = StringUtils.substringAfter(type, SpecializedTypesHandler.PROPERTY_PREFIX);
        int propType = PropertyType.valueFromName(type);
        DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
        // todo
        return null;
    }
}
