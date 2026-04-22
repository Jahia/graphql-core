/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdatev2;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * v2 of SchemaUpdateNodeExtensions.
 *
 * v2 changes vs v1:
 *  - schemaUpdateNodeTag   : value now prefixed with "v2-tag::" instead of "v1-tag::"
 *  - schemaUpdateNodeSecret: open in v1; v2 ADDS 'schemaUpdateNonExistent' permission → root loses access
 *                            root loses access after upgrade
 *  - schemaUpdateNodeGhost : non-existent permission 'schemaUpdateNonExistent' removed → now open
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Schema-update test node extensions – v2")
public class SchemaUpdateNodeExtensionsV2 {

    private final GqlJcrNode node;

    public SchemaUpdateNodeExtensionsV2(GqlJcrNode node) {
        this.node = node;
    }

    /**
     * CHANGED in v2: prefix is now "v2-tag::" instead of "v1-tag::".
     * Tests verify the new prefix is returned after upgrade and the v1 prefix is NOT returned.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeTag")
    @GraphQLDescription("Returns a v2-tagged label for this node. CHANGED in v2: prefix is 'v2-tag::'.")
    public String getNodeTag() {
        return "v2-tag::" + node.getName();
    }

    /**
     * CHANGED in v2: non-existent permission 'schemaUpdateNonExistent' added.
     * In v1 this field was open; after v2 root loses access, proving the schema was rebuilt.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeSecret")
    @GraphQLDescription("CHANGED in v2: permission changed to non-existent. Root is now denied.")
    @GraphQLRequiresPermission("schemaUpdateNonExistent")
    public String getNodeSecret() {
        return "v2-secret::" + node.getUuid();
    }

    /**
     * CHANGED in v2: non-existent permission 'schemaUpdateNonExistent' is removed.
     * In v1 root was denied; after upgrade to v2 this field is open and root gains access.
     */
    @GraphQLField
    @GraphQLName("schemaUpdateNodeGhost")
    @GraphQLDescription("Node ghost field – CHANGED in v2: permission removed. Now open – root gains access.")
    public String getNodeGhost() {
        return "v2-ghost::" + node.getName();
    }
}

