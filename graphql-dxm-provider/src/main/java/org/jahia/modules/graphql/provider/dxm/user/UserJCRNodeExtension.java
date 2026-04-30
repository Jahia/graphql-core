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
package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUserManagerService;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

/**
 * User metadata extensions for the JCR node.
 *
 * Adds resolved {@link GqlUser} fields for the common user-tracking properties on a JCR node
 * (createdBy, lastModifiedBy, lastPublishedBy, deletedBy), so that user display names, first names,
 * last names and email addresses are available in the same GraphQL request as the node data.
 */
@GraphQLTypeExtension(GqlJcrNode.class)
public class UserJCRNodeExtension {

    @Inject
    @GraphQLOsgiService
    private JahiaUserManagerService userManagerService;
    private final GqlJcrNode gqlJcrNode;

    public UserJCRNodeExtension(GqlJcrNode node) {
        this.gqlJcrNode = node;
    }

    /**
     * Resolves the user who created this node (jcr:createdBy).
     *
     * @return GqlUser for the creator, or null if the property is absent or the account no longer exists
     */
    @GraphQLField
    @GraphQLDescription("User who created this node (resolved from jcr:createdBy)")
    public GqlUser getCreatedByUser() {
        try {
            JCRNodeWrapper node = gqlJcrNode.getNode();
            if (!node.hasProperty("jcr:createdBy")) {
                return null;
            }
            String username = node.getProperty("jcr:createdBy").getString();
            return resolveUser(username, node);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Resolves the user who last modified this node (jcr:lastModifiedBy) in the given language.
     *
     * @param language Content language (required because jcr:lastModifiedBy is language-specific)
     * @return GqlUser for the last modifier, or null if the property is absent or the account no longer exists
     */
    @GraphQLField
    @GraphQLDescription("User who last modified this node (resolved from jcr:lastModifiedBy) for the given language")
    public GqlUser getLastModifiedByUser(
            @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Content language") String language) {
        return resolveI18nUserProperty("jcr:lastModifiedBy", language);
    }

    /**
     * Resolves the user who last published this node (j:lastPublishedBy) in the given language.
     *
     * @param language Content language (required because j:lastPublishedBy is language-specific)
     * @return GqlUser for the last publisher, or null if the property is absent or the account no longer exists
     */
    @GraphQLField
    @GraphQLDescription("User who last published this node (resolved from j:lastPublishedBy) for the given language")
    public GqlUser getLastPublishedByUser(
            @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Content language") String language) {
        return resolveI18nUserProperty("j:lastPublishedBy", language);
    }

    /**
     * Resolves the user who marked this node for deletion (j:deletionUser) in the given language.
     *
     * @param language Content language (required because j:deletionUser is language-specific)
     * @return GqlUser for the deleting user, or null if the property is absent or the account no longer exists
     */
    @GraphQLField
    @GraphQLDescription("User who marked this node for deletion (resolved from j:deletionUser) for the given language")
    public GqlUser getDeletedByUser(
            @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Content language") String language) {
        return resolveI18nUserProperty("j:deletionUser", language);
    }

    /**
     * Resolves the lock owner of this node (jcr:lockOwner).
     *
     * @return GqlUser for the lock owner, or null if the node is not locked or the account no longer exists
     */
    @GraphQLField
    @GraphQLDescription("User who holds the lock on this node (resolved from jcr:lockOwner)")
    public GqlUser getLockOwnerUser() {
        try {
            JCRNodeWrapper node = gqlJcrNode.getNode();
            if (!node.hasProperty("jcr:lockOwner")) {
                return null;
            }
            String username = node.getProperty("jcr:lockOwner").getString();
            return resolveUser(username, node);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    // -- helpers --

    private GqlUser resolveI18nUserProperty(String propertyName, String language) {
        try {
            JCRNodeWrapper i18nNode = NodeHelper.getNodeInLanguage(gqlJcrNode.getNode(), language);
            if (!i18nNode.hasProperty(propertyName)) {
                return null;
            }
            String username = i18nNode.getProperty(propertyName).getString();
            return resolveUser(username, gqlJcrNode.getNode());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    private GqlUser resolveUser(String username, JCRNodeWrapper node) throws RepositoryException {
        if (username == null || username.isEmpty()) {
            return null;
        }
        if (userManagerService == null) {
            userManagerService = BundleUtils.getOsgiService(JahiaUserManagerService.class, null);
        }
        String siteKey = getSiteKey(node);
        JCRUserNode userNode = userManagerService.lookupUser(username, siteKey);
        if (userNode == null) {
            return null;
        }
        return new GqlUser(userNode.getJahiaUser());
    }

    private String getSiteKey(JCRNodeWrapper node) {
        try {
            JCRSiteNode site = node.getResolveSite();
            return site != null ? site.getSiteKey() : null;
        } catch (RepositoryException e) {
            return null;
        }
    }
}
