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
package org.jahia.modules.graphql.provider.dxm.service.wip;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.Collection;

/**
 * GraphQL Work in progress informations.
 */
@GraphQLName("wipInfo")
@GraphQLDescription("Work in progress information")
public class GqlJcrWipInfo {

    private WipStatus status;
    private Collection<String> languages;

    public GqlJcrWipInfo(@GraphQLName("status") WipStatus status, @GraphQLName("languages") Collection<String> languages) {
        this.status = status;
        this.languages = languages;
    }

    @GraphQLField
    @GraphQLName("status")
    @GraphQLDescription("Get WIP status")
    public WipStatus getStatus() {
        return status;
    }

    /**
     * Returns the language of the content object to which the vanity URL maps to.
     *
     * @return language of the mapping
     */
    @GraphQLField
    @GraphQLName("languages")
    @GraphQLDescription("The languages set for Work in progress")
    public Collection<String> getLanguages() {
        return languages;
    }

    public enum WipStatus {
        @GraphQLDescription("Work in progress disabled")
        DISABLED,
        @GraphQLDescription("Work in progress for all languages")
        ALL_CONTENT,
        @GraphQLDescription("Work in progress for specified languages")
        LANGUAGES
    }
}
