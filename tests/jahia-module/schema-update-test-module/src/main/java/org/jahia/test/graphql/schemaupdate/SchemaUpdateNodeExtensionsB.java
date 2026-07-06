/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

/**
 * JCRNode extensions contributed by SchemaUpdateProviderB.
 *
 * Shared field (same name as Provider A, different annotations — tests permission addition):
 *   schemaUpdateNodeField – @GraphQLRequiresPermission non-existent → denied for everyone
 *                           A has no permission on this field → switching to B adds the restriction
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Schema-update test: Provider B node extensions")
public class SchemaUpdateNodeExtensionsB {

    private final GqlJcrNode node;

    public SchemaUpdateNodeExtensionsB(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLName("schemaUpdateNodeField")
    @GraphQLDescription("Node field (version B) — non-existent permission added → denied for everyone")
    @GraphQLRequiresPermission("schemaUpdateNonExistentFieldB")
    public String getNodeField() {
        return "node-B::" + node.getName();
    }
}
