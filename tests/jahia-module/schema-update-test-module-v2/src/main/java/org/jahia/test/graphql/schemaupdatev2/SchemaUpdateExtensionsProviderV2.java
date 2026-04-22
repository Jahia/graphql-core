/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdatev2;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Collection;

/**
 * v2 provider – same Bundle-SymbolicName as v1 so OSGi replaces it on upgrade.
 */
@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class SchemaUpdateExtensionsProviderV2 implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.asList(
                SchemaUpdateQueryExtensionsV2.class,
                SchemaUpdateNodeExtensionsV2.class
        );
    }
}

