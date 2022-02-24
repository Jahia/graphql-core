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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GraphQLDescription("Details on a lock")
public class GqlLockDetail {
    private static final Logger logger = LoggerFactory.getLogger(GqlLockDetail.class);
    private String language;
    private String owner;
    private String type;

    public GqlLockDetail(String language, String owner, String type) {
        this.language = language;
        this.owner = owner;
        this.type = type;
    }

    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("Language")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLName("owner")
    @GraphQLDescription("Lock owner")
    public String getOwner() {
        return owner;
    }

    @GraphQLField
    @GraphQLName("type")
    @GraphQLDescription("Lock type")
    public String getType() {
        return type;
    }
}
