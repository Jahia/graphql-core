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

import java.util.List;

@GraphQLName("TagMutationResult")
@GraphQLDescription("The result of a tag mutation")
public class GqlTagMutationResult {
    private final String tag;
    private final String nodeId;
    private final List<GqlTagWorkspaceMutationResult> workspaceResults;

    public GqlTagMutationResult(String tag, String nodeId, List<GqlTagWorkspaceMutationResult> workspaceResults) {
        this.tag = tag;
        this.nodeId = nodeId;
        this.workspaceResults = workspaceResults;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("The processed tag")
    public String getTag() {
        return tag;
    }

    @GraphQLField
    @GraphQLDescription("The impacted node identifier when the mutation targets a specific node")
    public String getNodeId() {
        return nodeId;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Per-workspace mutation results")
    public List<GqlTagWorkspaceMutationResult> getWorkspaceResults() {
        return workspaceResults;
    }
}
