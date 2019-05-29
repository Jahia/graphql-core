package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Field;

public interface PropertyFetcherExtensionInterface {
    DataFetcher getDataFetcher(Field field);
}
