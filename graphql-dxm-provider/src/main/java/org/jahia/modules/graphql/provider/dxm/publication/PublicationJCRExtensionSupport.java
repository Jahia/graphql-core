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
package org.jahia.modules.graphql.provider.dxm.publication;

import javax.jcr.RepositoryException;

import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.services.content.JCRSessionWrapper;

/**
 * Contains resources commonly used by GraphQL JCR publication extensions internally.
 */
public class PublicationJCRExtensionSupport {

    /**
     * Verify that a node belongs to the EDIT workspace; throw an exception if not.
     *
     * @param node The node to check
     */
    protected void validateNodeWorkspace(GqlJcrNode node) {
        JCRSessionWrapper session;
        try {
            session = node.getNode().getSession();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
        if (!session.getWorkspace().getName().equals(Constants.EDIT_WORKSPACE)) {
            throw new GqlJcrWrongInputException("Publication fields can only be used with nodes from " + NodeQueryExtensions.Workspace.EDIT + " workspace");
        }
    }
}
