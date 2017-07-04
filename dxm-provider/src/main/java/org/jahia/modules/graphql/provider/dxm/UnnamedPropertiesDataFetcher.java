package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLJCRNode;

import javax.jcr.PropertyType;

public class UnnamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String type = dataFetchingEnvironment.getFields().get(0).getName();
        type = StringUtils.substringAfter(type, JCRNodeTypeResolver.PROPERTY_PREFIX);
        int propType = PropertyType.valueFromName(type);
        DXGraphQLJCRNode node = (DXGraphQLJCRNode) dataFetchingEnvironment.getSource();
        // todo
        return null;
    }
}
