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
package org.jahia.modules.graphql.provider.dxm.service.tags.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import java.util.List;

@GraphQLName("TagWorkspaceMutationResult")
@GraphQLDescription("The result of a tag mutation for one workspace")
public class GqlTagWorkspaceMutationResult {
    private final String workspace;
    private final int processedCount;
    private final List<GqlJcrNode> updatedNodes;
    private final List<GqlJcrNode> failedNodes;

    public GqlTagWorkspaceMutationResult(String workspace, int processedCount, List<GqlJcrNode> updatedNodes, List<GqlJcrNode> failedNodes) {
        this.workspace = workspace;
        this.processedCount = processedCount;
        this.updatedNodes = updatedNodes;
        this.failedNodes = failedNodes;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("The workspace that was updated")
    public String getWorkspace() {
        return workspace;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("How many nodes were updated in this workspace")
    public Integer getProcessedCount() {
        return processedCount;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Nodes updated during this workspace mutation")
    public List<GqlJcrNode> getUpdatedNodes() {
        return updatedNodes;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Nodes that failed to update during this workspace mutation")
    public List<GqlJcrNode> getFailedNodes() {
        return failedNodes;
    }
}
