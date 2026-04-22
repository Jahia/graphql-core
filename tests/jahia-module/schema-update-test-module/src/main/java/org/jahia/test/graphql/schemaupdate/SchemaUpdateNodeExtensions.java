/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * Extension on GqlJcrNode contributed by schema-update-test-module.
 *
 * Adds:
 *  - schemaUpdateNodeTag      : open field (no permission)
 *  - schemaUpdateNodeSecret   : open field in v1; in v2 a non-existent permission is added (root loses access)
 *  - schemaUpdateNodeGhost    : denied field (non-existent permission); in v2 permission removed (root gains access)
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Schema-update test node extensions")
public class SchemaUpdateNodeExtensions {

    private final GqlJcrNode node;

    public SchemaUpdateNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    /** Open field; prefix "v1-tag::" distinguishes v1 resolver from v2. */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeTag")
    @GraphQLDescription("Returns a tagged label for this node. No permission required.")
    public String getNodeTag() {
        return "v1-tag::" + node.getName();
    }

    /**
     * Open field in v1 (no permission).
     * In v2 a non-existent permission is added so root loses access, proving the schema was rebuilt.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeSecret")
    @GraphQLDescription("Returns a secret value for this node. No permission required in v1.")
    public String getNodeSecret() {
        return "v1-secret::" + node.getUuid();
    }

    /**
     * Field with a non-existent permission – denied for every user including root.
     * In v2 the permission is removed so root gains access, proving the schema was rebuilt.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeGhost")
    @GraphQLDescription("Returns a ghost value. Requires 'schemaUpdateNonExistent' – denied for everyone.")
    @GraphQLRequiresPermission("schemaUpdateNonExistent")
    public String getNodeGhost() {
        return "ghost::" + node.getName();
    }
}

