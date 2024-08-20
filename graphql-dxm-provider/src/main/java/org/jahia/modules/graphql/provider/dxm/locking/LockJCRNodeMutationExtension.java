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
package org.jahia.modules.graphql.provider.dxm.locking;

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclService;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.usermanager.JahiaUser;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.function.Supplier;

/**
 * Lock mutation extensions for JCR node.
 */
@GraphQLTypeExtension(GqlJcrNodeMutation.class)
public class LockJCRNodeMutationExtension {

    private GqlJcrNodeMutation nodeMutation;

    /**
     * Create a lock mutation extension instance.
     *
     * @param nodeMutation JCR node mutation to apply the extension to
     */
    @Inject
    public LockJCRNodeMutationExtension(GqlJcrNodeMutation nodeMutation) {
        this.nodeMutation = nodeMutation;
        this.aclService = BundleUtils.getOsgiService(JahiaAclService.class, null);
    }

    private JahiaAclService aclService;

    /**
     * Lock the node.
     *
     * @return True if lock operation was successful and false if it wasn't
     */
    @GraphQLField
    @GraphQLDescription("Lock the node")
    public boolean lock(@GraphQLName("type") @GraphQLDefaultValue(DefaultLockTypeProvider.class) @GraphQLDescription("Type of lock, defaults to user") String type) {
        try {
            JCRNodeWrapper nodeToLock = nodeMutation.getNode().getNode();
            return nodeToLock.lockAndStoreToken(type);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Unlock the node.
     *
     * @return True if unlock operation was successful and false if it wasn't
     */
    @GraphQLField
    @GraphQLDescription("Unlock the node")
    public boolean unlock(
            @GraphQLName("type") @GraphQLDefaultValue(DefaultLockTypeProvider.class) @GraphQLDescription("Type of lock, defaults to user") String type
    ) {
        try {
            JCRNodeWrapper nodeToUnlock = nodeMutation.getNode().getNode();
            if (!nodeToUnlock.isLocked()) {
                return false;
            }
            if (nodeToUnlock.getSession().getUser().isRoot() || isUserSiteAdmin()) {
                String lockOwner = nodeToUnlock.getLockOwner();
                nodeToUnlock.unlock(type, lockOwner);
            }
            else {
                nodeToUnlock.unlock(type);
            }
            return !nodeToUnlock.isLocked();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Unlock the node and sub nodes.
     *
     * @return True if unlock operation was successful and false if it wasn't
     */
    @GraphQLField
    @GraphQLDescription("Unlock all nodes under the specified node")
    public boolean clearAllLocks() {
        try {
            JCRNodeWrapper nodeToUnlock = nodeMutation.getNode().getNode();
            if (nodeToUnlock.hasPermission("clearLock")) {
                //Retrieve the system session in order to remove the locks.
                JCRSessionWrapper systemSession = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, nodeToUnlock.getSession().getLocale(), nodeToUnlock.getSession().getFallbackLocale());
                systemSession.getNode(nodeToUnlock.getPath()).clearAllLocks();
            }
            return !nodeToUnlock.isLocked();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    public static class DefaultLockTypeProvider implements Supplier<Object> {
        @Override
        public String get() {
            return "user";
        }
    }

    private boolean isUserSiteAdmin() throws RepositoryException {
        JahiaUser user = this.nodeMutation.jcrNode.getSession().getUser();
        return aclService.hasInheritedUserRole(this.nodeMutation.jcrNode, user, "site-administrator");
    }
}
