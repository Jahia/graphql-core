/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Collection;

/**
 * OSGi component that registers the schema-update-test-module extensions with
 * the DX GraphQL provider.
 *
 * When this bundle is installed, the provider reactivates (policy=STATIC) and
 * adds SchemaUpdateQueryExtensions + SchemaUpdateNodeExtensions to the schema.
 * When the bundle is uninstalled, the provider reactivates again and those
 * types / fields disappear from the schema.
 */
@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class SchemaUpdateExtensionsProvider implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.asList(
                SchemaUpdateQueryExtensions.class,
                SchemaUpdateNodeExtensions.class
        );
    }
}

