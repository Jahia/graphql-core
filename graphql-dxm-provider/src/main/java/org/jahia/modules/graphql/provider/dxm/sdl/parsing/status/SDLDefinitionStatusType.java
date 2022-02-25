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
package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public enum SDLDefinitionStatusType {
    OK("OK"),
    MISSING_FIELDS("Type does not define any field"),
    MISSING_TYPE("%s gql type is unavailable"),
    MISSING_JCR_NODE_TYPE("%s node type was not found"),
    MISSING_JCR_PROPERTY("%s property is missing from node type"),
    MISSING_JCR_CHILD("%s child is missing from node type"),
    MISSING_FETCHER("fetcher %s does not exist or is not registered"),
    MISSING_FETCHER_ARGUMENT("fetcher %s argument is missing for field %s");
    private String message;

    SDLDefinitionStatusType(String message) {
        this.message = message;
    }

    public String getMessage(String ...param) {
        return String.format(message, param);
    }
}
