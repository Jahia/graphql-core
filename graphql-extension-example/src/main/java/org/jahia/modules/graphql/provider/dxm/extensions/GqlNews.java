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
package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;

@GraphQLDescription("Sample GQLNews type")
public class GqlNews  {

    private  GqlJcrNodeImpl node;

    public GqlNews(JCRNodeWrapper node) {
        this.node = new GqlJcrNodeImpl(node);
    }

    @GraphQLField
    @GraphQLDescription("News uuid field")
    public String getUuid() {
        return node.getUuid();
    }

    @GraphQLField
    @GraphQLDescription("News description field")
    public String getDescription(@GraphQLName("language") @GraphQLDescription("News language argument") @GraphQLNonNull String language) {
        return node.getProperty("desc",language, false).getValue();
    }

    @GraphQLField
    @GraphQLDescription("News title field")
    public String getTitle(@GraphQLName("language") @GraphQLDescription("News language argument") @GraphQLNonNull String language) {
        return node.getProperty("jcr:title",language, false).getValue();
    }

    @GraphQLField
    @GraphQLDescription("News file field")
    public GqlJcrNode getFile() {
        return node.getProperty("date", null, false).getRefNode();
    }

    @GraphQLField
    @GraphQLDescription("News date field")
    public String getDate() {
        return node.getProperty("date", null, false).getValue();
    }

}
