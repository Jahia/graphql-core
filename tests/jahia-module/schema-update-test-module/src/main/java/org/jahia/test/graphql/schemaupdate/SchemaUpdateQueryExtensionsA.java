/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * Query extensions contributed by SchemaUpdateProviderA.
 *
 * Shared fields (same name as Provider B, different annotations — tests schema evolution):
 *   schemaUpdatePing   – open, description "Ping operation (version A)"
 *   schemaUpdateGated  – @GraphQLRequiresPermission non-existent → denied for everyone
 *                        B removes this permission → field becomes accessible
 *
 * A-only field (disappears when switching to B):
 *   schemaUpdateAOnly  – open, unique to Provider A
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Schema-update test: Provider A query extensions")
public class SchemaUpdateQueryExtensionsA {

    @GraphQLField
    @GraphQLName("schemaUpdatePing")
    @GraphQLDescription("Ping operation (version A)")
    public static String ping(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "pong-A" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }

    @GraphQLField
    @GraphQLName("schemaUpdateGated")
    @GraphQLDescription("Gated operation")
    @GraphQLRequiresPermission("schemaUpdateNonExistentFieldA")
    public static String gated(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "gated-A" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }

    @GraphQLField
    @GraphQLName("schemaUpdateAOnly")
    @GraphQLDescription("Field only present in Provider A — absent when Provider B is active")
    public static String aOnly(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "a-only" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }
}
