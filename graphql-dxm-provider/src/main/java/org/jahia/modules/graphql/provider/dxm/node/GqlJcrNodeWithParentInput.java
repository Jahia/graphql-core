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

@GraphQLName("JCRNodeWithParent")
@GraphQLDescription("GraphQL representation of a JCR node to be created")
public class GqlJcrNodeWithParentInput extends GqlJcrNodeInput {

    private String parentPathOrId;

    public GqlJcrNodeWithParentInput(@GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The parent path or id where the node will be created") String parentPathOrId,
                                     @GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the node to create") String name,
                                     @GraphQLName("primaryNodeType") @GraphQLDescription("The primary node type of the node to create") @GraphQLNonNull String primaryNodeType,
                                     @GraphQLName("useAvailableNodeName") @GraphQLDescription("If true, use the next available name for a node, appending if needed numbers. Default is false") Boolean useAvailableNodeName,
                                     @GraphQLName("mixins") @GraphQLDescription("The collection of mixins to add to the node") Collection<String> mixins,
                                     @GraphQLName("properties") @GraphQLDescription("The collection of properties to set to the node") Collection<GqlJcrPropertyInput> properties,
                                     @GraphQLName("children") @GraphQLDescription("The collection of sub nodes to create") Collection<GqlJcrNodeInput> children) {
        super(name, primaryNodeType, useAvailableNodeName, mixins, properties, children);
        this.parentPathOrId = parentPathOrId;
    }

    @GraphQLField
    @GraphQLName("parentPathOrId")
    @GraphQLNonNull
    @GraphQLDescription("The parent path or id where the node will be created")
    public String getParentPathOrId() {
        return parentPathOrId;
    }
}
