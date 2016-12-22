package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;

import javax.jcr.PropertyType;

class UnnamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = StringUtils.substringAfter(type, DXGraphQLNodeBuilder.PROPERTY_PREFIX);
        int propType = PropertyType.valueFromName(type);
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        // todo
        return null;
    }
}
