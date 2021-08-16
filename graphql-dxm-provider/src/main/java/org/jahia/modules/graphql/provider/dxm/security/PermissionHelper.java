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
package org.jahia.modules.graphql.provider.dxm.security;

import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.securityfilter.PermissionService;

import javax.jcr.RepositoryException;

public class PermissionHelper {

    private PermissionHelper() {
    }

    public static boolean hasPermission(JCRNodeWrapper node, DataFetchingEnvironment environment) {
        if (ContextUtil.getHttpServletRequest(environment.getContext()) != null) {
            PermissionService permissionService = BundleUtils.getOsgiService(PermissionService.class, null);
            if (permissionService == null) {
                throw new DataFetchingException("Could not find permission service to validate security access. Blocking access to data.");
            }
            try {
                return permissionService.hasPermission("graphql." + GqlTypeUtil.getTypeName(environment.getParentType()) + "." + environment.getFieldDefinition().getName(), node);
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        } else {
            return true;
        }
    }
}
