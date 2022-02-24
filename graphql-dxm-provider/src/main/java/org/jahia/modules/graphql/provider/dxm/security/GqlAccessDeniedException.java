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

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;

/**
 * Exception throw when access is denied for the requested resource
 */
public class GqlAccessDeniedException extends BaseGqlClientException {

    private static final long serialVersionUID = 549689671626148400L;

    private String permission;

    GqlAccessDeniedException(String permission) {
        super("Permission denied", null);
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
