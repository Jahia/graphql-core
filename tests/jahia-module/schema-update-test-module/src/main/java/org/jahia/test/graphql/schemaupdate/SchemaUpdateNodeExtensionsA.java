/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 * Licensed under the Apache License, Version 2.0
 */
package org.jahia.test.graphql.schemaupdate;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

/**
 * JCRNode extensions contributed by SchemaUpdateProviderA.
 *
 * Shared field (same name as Provider B, different annotations — tests permission addition):
 *   schemaUpdateNodeField – open in A; Provider B adds @GraphQLRequiresPermission → denied
 */
@GraphQLTypeExtension(GqlJcrNode.class)
@GraphQLDescription("Schema-update test: Provider A node extensions")
public class SchemaUpdateNodeExtensionsA {

    private final GqlJcrNode node;

    public SchemaUpdateNodeExtensionsA(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLName("schemaUpdateNodeField")
    @GraphQLDescription("Node field (version A) — open, no permission required")
    public String getNodeField() {
        return "node-A::" + node.getName();
    }
}
