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
 * Provider A — activated by a test via ConfigAdmin (configurationPolicy = REQUIRE).
 *
 * Contributes:
 *  - schemaUpdateAPing        : open Query field
 *  - schemaUpdateAGhostPing   : Query field with non-existent permission (always denied)
 *  - schemaUpdateANodeTag     : open JCRNode field
 *
 * The explicit component name doubles as the OSGi configuration PID; tests use
 * that PID to create/delete the configuration that controls activation.
 */
@Component(
        service = DXGraphQLExtensionsProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        name = "org.jahia.test.graphql.schemaupdate.providerA",
        immediate = true
)
public class SchemaUpdateProviderA implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.asList(
                SchemaUpdateQueryExtensionsA.class,
                SchemaUpdateNodeExtensionsA.class
        );
    }
}
