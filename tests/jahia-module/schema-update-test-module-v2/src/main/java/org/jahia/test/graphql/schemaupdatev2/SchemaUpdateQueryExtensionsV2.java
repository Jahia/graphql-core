/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdatev2;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * v2 of SchemaUpdateQueryExtensions.
 *
 * v2 changes vs v1:
 *  - schemaUpdatePing      : implementation updated, returns "v2-pong" prefix. Still open.
 *  - schemaUpdateAdminPing : argument renamed from "message" to "text"; returns "v2-admin-pong". Still open.
 *  - schemaUpdateGhostPing : non-existent permission removed → now open; root gains access.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Schema-update test query extensions – v2")
public class SchemaUpdateQueryExtensionsV2 {

    /** CHANGED in v2: implementation returns "v2-pong" prefix. Still open (no permission). */
    @GraphQLField
    @GraphQLName("schemaUpdatePing")
    @GraphQLDescription("Returns a greeting. CHANGED in v2: updated prefix.")
    public static String ping(
            @GraphQLName("message") @GraphQLDescription("Optional message to echo back") String message) {
        return "v2-pong" + (message != null && !message.isEmpty() ? ": " + message : "");
    }

    /**
     * CHANGED in v2: argument renamed from "message" to "text". Still open (no permission).
     * Old clients passing "message" will get a schema validation error.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateAdminPing")
    @GraphQLDescription("Returns a greeting. CHANGED in v2: argument renamed to 'text'.")
    public static String adminPing(
            @GraphQLName("text") @GraphQLDescription("Message text (renamed from 'message' in v1)") String text) {
        return "v2-admin-pong" + (text != null && !text.isEmpty() ? ": " + text : "");
    }

    /**
     * CHANGED in v2: non-existent permission 'schemaUpdateNonExistent' removed.
     * In v1 root was denied; after v2 this field is open and root gains access.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateGhostPing")
    @GraphQLDescription("CHANGED in v2: permission removed. Now open – root gains access.")
    public static String ghostPing(
            @GraphQLName("message") @GraphQLDescription("Optional message to echo back") String message) {
        return "v2-ghost-pong" + (message != null && !message.isEmpty() ? ": " + message : "");
    }
}

