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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import org.apache.commons.beanutils.PropertyUtils;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationTargetException;
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
                vanityUrl.setUrl(url);
            }
            vanityUrl.setFile(targetNode.isFile());
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
