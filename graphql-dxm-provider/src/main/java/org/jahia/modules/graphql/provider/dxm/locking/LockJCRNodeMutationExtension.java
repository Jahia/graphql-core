/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.locking;

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.JCRSessionWrapper;

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
    public LockJCRNodeMutationExtension(GqlJcrNodeMutation nodeMutation) throws GqlJcrWrongInputException {
        this.nodeMutation = nodeMutation;
    }

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
    public boolean unlock(@GraphQLName("type") @GraphQLDefaultValue(DefaultLockTypeProvider.class) @GraphQLDescription("Type of lock, defaults to user") String type) {
        try {
            JCRNodeWrapper nodeToUnlock = nodeMutation.getNode().getNode();
            if (!nodeToUnlock.isLocked()) {
                return false;
            }
            if (nodeToUnlock.getSession().getUser().isRoot()) {
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
            if (nodeToUnlock.getSession().getUser().isRoot()) {
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
}
