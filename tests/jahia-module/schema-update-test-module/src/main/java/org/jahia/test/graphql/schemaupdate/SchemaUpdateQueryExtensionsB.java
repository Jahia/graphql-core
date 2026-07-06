/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * Query extensions contributed by SchemaUpdateProviderB.
 *
 * Shared fields (same name as Provider A, different annotations — tests schema evolution):
 *   schemaUpdatePing   – open, description "Ping operation (version B)" (description changed)
 *   schemaUpdateGated  – no @GraphQLRequiresPermission (permission removed → now accessible)
 *
 * B-only field (appears when switching from A):
 *   schemaUpdateBOnly  – open, unique to Provider B
 *
 * DXGraphQLConfig test target:
 *   schemaUpdatePing is also used in Scenario 4 to verify that a runtime config-based
 *   permission can deny and re-allow access without a schema rebuild.
 *
 * Real-permission test target (Scenario 6):
 *   schemaUpdatePermTest – @GraphQLRequiresPermission("schemaUpdateTestAccess") — a real Jahia
 *   permission created in JCR for the test. Users with a role granting this permission can access
 *   it; others get GqlAccessDeniedException.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Schema-update test: Provider B query extensions")
public class SchemaUpdateQueryExtensionsB {

    @GraphQLField
    @GraphQLName("schemaUpdatePing")
    @GraphQLDescription("Ping operation (version B)")
    public static String ping(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "pong-B" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }

    @GraphQLField
    @GraphQLName("schemaUpdateGated")
    @GraphQLDescription("Gated operation")
    public static String gated(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "gated-B" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }

    @GraphQLField
    @GraphQLName("schemaUpdateBOnly")
    @GraphQLDescription("Field only present in Provider B — absent when Provider A is active")
    public static String bOnly(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "b-only" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }

    @GraphQLField
    @GraphQLName("schemaUpdatePermTest")
    @GraphQLDescription("Field requiring real Jahia permission 'schemaUpdateTestAccess' — Scenario 6")
    @GraphQLRequiresPermission("schemaUpdateTestAccess")
    public static String permTest(
            @GraphQLName("msg") @GraphQLDescription("Message to echo") String msg) {
        return "perm-test" + (msg != null && !msg.isEmpty() ? ": " + msg : "");
    }
}
