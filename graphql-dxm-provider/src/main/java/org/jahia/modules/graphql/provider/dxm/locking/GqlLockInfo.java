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