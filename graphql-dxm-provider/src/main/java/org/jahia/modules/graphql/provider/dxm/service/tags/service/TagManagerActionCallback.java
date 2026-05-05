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
package org.jahia.modules.graphql.provider.dxm.service.tags.service;

import org.jahia.modules.graphql.provider.dxm.service.tags.graphql.GqlTagWorkspaceMutationResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.tags.TagActionCallback;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

class TagManagerActionCallback implements TagActionCallback<GqlTagWorkspaceMutationResult> {
    private static final int SAVE_BATCH_SIZE = 100;

    private final JCRSessionWrapper session;
    private final String workspace;
    private final List<String> failedPaths = new ArrayList<>();
    private int processedCount;
    private int failedCount;

    TagManagerActionCallback(JCRSessionWrapper session, String workspace) {
        this.session = session;
        this.workspace = workspace;
    }

    @Override
    public void afterTagAction(JCRNodeWrapper node) throws RepositoryException {
        processedCount++;
        if (processedCount % SAVE_BATCH_SIZE == 0) {
            session.save();
        }

        TagManagerServiceSupport.flushNodeCaches(node.getPath());
    }

    @Override
    public void onError(JCRNodeWrapper node, RepositoryException e) throws RepositoryException {
        failedCount++;
        if (failedPaths.size() < GqlTagWorkspaceMutationResult.MAX_REPORTED_FAILURES) {
            failedPaths.add(node.getPath());
        }
    }

    @Override
    public GqlTagWorkspaceMutationResult end() throws RepositoryException {
        session.save();
        return new GqlTagWorkspaceMutationResult(workspace, processedCount, failedCount, failedPaths);
    }
}
