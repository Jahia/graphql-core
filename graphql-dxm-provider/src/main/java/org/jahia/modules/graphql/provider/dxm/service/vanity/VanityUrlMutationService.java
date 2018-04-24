package org.jahia.modules.graphql.provider.dxm.service.vanity;
/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralize vanity url related operations
 */
class VanityUrlMutationService {

    private JCRNodeWrapper targetNode;
    private VanityUrlService vanityUrlService;

    public VanityUrlMutationService(JCRNodeWrapper targetNode, VanityUrlService vanityUrlService) {
        this.targetNode = targetNode;
        this.vanityUrlService = vanityUrlService;
    }

    /**
     * update a vanity URL.
     *
     * @param vanityUrl vanity url to update
     * @param active Desired value of the active flag or null to keep existing value
     * @param defaultMapping Desired value of the default flag or null to keep existing value
     * @param language Desired vanity URL language or null to keep existing value
     * @param url Desired URL value or null to keep existing value
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    boolean update(VanityUrl vanityUrl, Boolean active, Boolean defaultMapping, String language, String url) {
        addOrUpdateVanity(vanityUrl, active, defaultMapping, language, url);
        return true;
    }

    /**
     * Add vanity urls
     * @param vanityUrls List of vanity urls to add
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    boolean add(List<GqlJcrVanityUrlInput> vanityUrls) {

        Map<String, Object> errors = new HashMap<>();

        for (GqlJcrVanityUrlInput vanityUrl : vanityUrls) {
            try {
                VanityUrl v = new VanityUrl();
                v.setSite(targetNode.getResolveSite().getSiteKey());
                addOrUpdateVanity(v, vanityUrl.isActive(), vanityUrl.isDefaultMapping(), vanityUrl.getLanguage(), vanityUrl.getUrl());
            } catch (GqlConstraintViolationException e) {
                errors.put(vanityUrl.getUrl(), e.getExtensions());
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new GqlConstraintViolationException(new Exception("unable to create vanity urls"), errors);
        }
        return true;

    }

    private void addOrUpdateVanity(VanityUrl vanityUrl, Boolean active, Boolean defaultMapping, String language, String url) throws GqlConstraintViolationException {
        try {
            if (active != null) {
                vanityUrl.setActive(active);
            }
            if (defaultMapping != null) {
                vanityUrl.setDefaultMapping(defaultMapping);
            }
            if (language != null) {
                vanityUrl.setLanguage(language);
            }
            if (url != null) {
                vanityUrl.setUrl(StringUtils.stripEnd(StringUtils.startsWith(url, "/") ? url : '/' + url, "/"));
            }
            vanityUrlService.saveVanityUrlMapping(targetNode, vanityUrl, false);
        } catch (NonUniqueUrlMappingException e) {
            Map<String, Object> extensions = new HashMap<>();
            extensions.put("type", e.getClass().getName());
            extensions.put("existingNodePath", e.getExistingNodePath());
            extensions.put("urlMapping", e.getUrlMapping());
            extensions.put("workspace", e.getWorkspace());
            extensions.put("nodePath", e.getNodePath());
            throw new GqlConstraintViolationException(e, extensions);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
