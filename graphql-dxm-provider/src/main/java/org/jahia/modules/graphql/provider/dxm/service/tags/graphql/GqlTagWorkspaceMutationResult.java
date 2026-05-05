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

/**
 * Lightweight summary of a tag mutation applied to a single JCR workspace.
 *
 * <p>The result intentionally avoids carrying full {@code GqlJcrNode} objects for updated nodes.
 * Returning a potentially unbounded list of heavy node instances for the success path would load
 * large amounts of data into memory on sites with many tagged nodes, while providing little value
 * to callers — the UI only needs a count to display "X nodes updated". A separate query using the
 * returned {@link #getFailedPaths()} can fetch full node details on demand when recovery action
 * is needed.
 *
 * <p>For the failure path, at most {@value #MAX_REPORTED_FAILURES} node paths are included in the
 * payload. The total count is always available via {@link #getFailedCount()}. Callers that need to
 * display or process more failures should paginate using a follow-up tagged-content query.
 */
@GraphQLName("TagWorkspaceMutationResult")
@GraphQLDescription("The result of a tag mutation for one workspace")
public class GqlTagWorkspaceMutationResult {

    /**
     * Maximum number of failure paths included in a single response payload.
     * Additional failures are counted in {@link #getFailedCount()} but not listed.
     */
    public static final int MAX_REPORTED_FAILURES = 10;

    private final String workspace;
    private final int processedCount;
    private final int failedCount;
    private final List<String> failedPaths;

    public GqlTagWorkspaceMutationResult(String workspace, int processedCount, int failedCount, List<String> failedPaths) {
        this.workspace = workspace;
        this.processedCount = processedCount;
        this.failedCount = failedCount;
        this.failedPaths = failedPaths;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("The workspace that was updated")
    public String getWorkspace() {
        return workspace;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Number of nodes successfully updated in this workspace")
    public Integer getProcessedCount() {
        return processedCount;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Total number of nodes that failed to update in this workspace (may exceed the size of failedPaths)")
    public Integer getFailedCount() {
        return failedCount;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("JCR paths of nodes that failed to update, capped at " + MAX_REPORTED_FAILURES +
            ". Use a follow-up taggedContent query with these paths for full node details.")
    public List<String> getFailedPaths() {
        return failedPaths;
    }
}
