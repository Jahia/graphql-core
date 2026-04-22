/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * Query-level type extension contributed by schema-update-test-module.
 *
 * Adds:
 *  - schemaUpdatePing        : open field (no permission)
 *  - schemaUpdateAdminPing   : open field (no permission); used to test arg rename in v2
 *  - schemaUpdateGhostPing   : denied field (@GraphQLRequiresPermission with non-existent name)
 *
 * Note: in Jahia, hasPermission() returns false for root on any custom (non-JCR) permission,
 * so @GraphQLRequiresPermission always denies root regardless of the permission name.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Schema-update test query extensions")
public class SchemaUpdateQueryExtensions {

    /**
     * Open field – any authenticated user can call it.
     */
    @GraphQLField
    @GraphQLName("schemaUpdatePing")
    @GraphQLDescription("Returns a greeting. No permission required.")
    public static String ping(@GraphQLName("message") @GraphQLDescription("Optional message to echo back") String message) {
        return "pong" + (message != null && !message.isEmpty() ? ": " + message : "");
    }

    /**
     * Open field – used to test argument renaming between v1 and v2.
     * In v2 the argument is renamed from "message" to "text".
     * No permission required in either version.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateAdminPing")
    @GraphQLDescription("Returns a greeting (arg 'message'). No permission required.")
    public static String adminPing(@GraphQLName("message") @GraphQLDescription("Optional message to echo back") String message) {
        return "admin-pong" + (message != null && !message.isEmpty() ? ": " + message : "");
    }

    /**
     * Field with a non-existent permission – denied for every user including root.
     * In v2 the permission is removed so root gains access; this proves the schema was rebuilt.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateGhostPing")
    @GraphQLDescription("Returns a ghost greeting. Requires 'schemaUpdateNonExistent' – denied for everyone.")
    @GraphQLRequiresPermission("schemaUpdateNonExistent")
    public static String ghostPing(@GraphQLName("message") @GraphQLDescription("Optional message to echo back") String message) {
        return "ghost-pong" + (message != null && !message.isEmpty() ? ": " + message : "");
    }
}

