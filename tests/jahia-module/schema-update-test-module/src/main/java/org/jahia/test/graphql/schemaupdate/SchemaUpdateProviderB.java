/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.util.Arrays;
import java.util.Collection;

/**
 * Provider B — activated by a test via ConfigAdmin (configurationPolicy = REQUIRE).
 *
 * Contributes:
 *  - schemaUpdateBPing       : open Query field (arg name "txt" to distinguish from A's "msg")
 *  - schemaUpdateBAccess     : open Query field used for DXGraphQLConfig permission test
 *  - schemaUpdateBNodeGhost  : JCRNode field with non-existent permission (always denied)
 *
 * The explicit component name doubles as the OSGi configuration PID.
 */
@Component(
        service = DXGraphQLExtensionsProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        name = "org.jahia.test.graphql.schemaupdate.providerB",
        immediate = true
)
public class SchemaUpdateProviderB implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.asList(
                SchemaUpdateQueryExtensionsB.class,
                SchemaUpdateNodeExtensionsB.class
        );
    }
}
