package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;

public class PropertiesDataFetcherFactory {

    public static DataFetcher getFetcher(Field field) {
        switch(field.getName()) {
            default: return new PropertiesDataFetcher(field);
        }
    }
}
