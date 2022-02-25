/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Collection;

@GraphQLName("JCRNode")
@GraphQLDescription("GraphQL representation of a JCR node to be created")
public class GqlJcrNodeInput {

    private String name;
    private String primaryNodeType;
    private Boolean useAvailableNodeName;
    private Collection<String> mixins;
    private Collection<GqlJcrPropertyInput> properties;
    private Collection<GqlJcrNodeInput> children;

    public GqlJcrNodeInput(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create") String name,
                           @GraphQLName("primaryNodeType") @GraphQLNonNull @GraphQLDescription("The primary node type of the node to create") String primaryNodeType,
                           @GraphQLName("useAvailableNodeName") @GraphQLDescription("If true, use the next available name for a node, appending if needed numbers. Default is false") Boolean useAvailableNodeName,
                           @GraphQLName("mixins") @GraphQLDescription("The collection of mixins to add to the node") Collection<String> mixins,
                           @GraphQLName("properties") @GraphQLDescription("The collection of properties to set to the node") Collection<GqlJcrPropertyInput> properties,
                           @GraphQLName("children") @GraphQLDescription("The collection of sub nodes to create") Collection<GqlJcrNodeInput> children) {
        this.name = name;
        this.primaryNodeType = primaryNodeType;
        this.useAvailableNodeName = useAvailableNodeName;
        this.mixins = mixins;
        this.properties = properties;
        this.children = children;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("The name of the node to create")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("useAvailableNodeName")
    @GraphQLDescription("If true, use the next available name for a node, appending if needed numbers. Default is false")
    public Boolean useAvailableNodeName() {
        return useAvailableNodeName;
    }

    @GraphQLField
    @GraphQLName("primaryNodeType")
    @GraphQLNonNull
    @GraphQLDescription("The primary node type of the node to create")
    public String getPrimaryNodeType() {
        return primaryNodeType;
    }

    @GraphQLField
    @GraphQLName("mixins")
    @GraphQLDescription("The collection of mixins to add to the node")
    public Collection<String> getMixins() {
        return mixins;
    }

    @GraphQLField
    @GraphQLName("properties")
    @GraphQLDescription("The collection of properties to set to the node")
    public Collection<GqlJcrPropertyInput> getProperties() {
        return properties;
    }

    @GraphQLField
    @GraphQLName("children")
    @GraphQLDescription("The collection of sub nodes to create")
    public Collection<GqlJcrNodeInput> getChildren() {
        return children;
    }
}
