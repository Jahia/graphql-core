package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * TODO Comment me
 *
 * @author toto
 */
class IdentityDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
