/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@GraphQLName("LockInfo")
@GraphQLDescription("Information on node lock")
public class GqlLockInfo {
    public static final Logger logger = LoggerFactory.getLogger(GqlLockInfo.class);

    private JCRNodeWrapper node;

    public GqlLockInfo(JCRNodeWrapper node) {
        this.node = node;
    }

    @GraphQLField
    @GraphQLDescription("Is node lockable")
    public boolean isLockable() {
        return node.isLockable();
    }

    @GraphQLField
    @GraphQLDescription("Can the node be locked")
    public boolean canLock() {
        try {
            if (isLockable()) {
                JahiaUser jahiaUser = node.getSession().getUser();
                if (jahiaUser.isRoot()) {
                    return true;
                }
                return isUserLockOwner();
            }
        } catch(RepositoryException ex) {
            logger.error("Failed to access repository", ex);
            return false;
        }
        return false;
    }

    @GraphQLField
    @GraphQLDescription("Can the node be unlocked")
    public boolean canUnlock() {
        try {
            if (node.isLocked()) {
                JahiaUser jahiaUser = node.getSession().getUser();
                if (jahiaUser.isRoot()) {
                    return true;
                }
                return isUserLockOwner();
            }
        } catch(RepositoryException ex) {
            logger.error("Failed to access repository", ex);
            return false;
        }
        return false;
    }

    @GraphQLField
    @GraphQLName("details")
    @GraphQLDescription("Is node lockable")
    public List<GqlLockDetail> getDetails(@GraphQLName("language") @GraphQLDescription("language in which to retrieve details") String language) {
        List<GqlLockDetail> lockDetails = new LinkedList<>();
        try {
            Map<String, List<String>> lockInfos = node.getLockInfos();
            List<String> lockInfo;
            if (language != null && lockInfos.containsKey(language)) {
                //Retrieve info for i18n language node
                lockInfo = lockInfos.get(language);
            } else {
                //Retrieve default lock info
                language = null;
                lockInfo = lockInfos.get(null);
            }
            if (lockInfo != null) {
                for (String lock : lockInfo) {
                    lockDetails.add(new GqlLockDetail(language, StringUtils.substringBefore(lock, ":"), StringUtils.substringAfter(lock, ":")));
                }
            }
        } catch(RepositoryException ex) {
            logger.error("Failed to access repository", ex);
        }
        return lockDetails;
    }

    private boolean isUserLockOwner() throws RepositoryException {
        if (node.getLock().getLockOwner() != null) {
            String[] lockOwners = node.getLock().getLockOwner().split(" ");
            for(String lockOwner : lockOwners) {
                if (node.getSession().getUserID().equals(lockOwner)) {
                    return true;
                }
            }
        }
        return false;
    }
}