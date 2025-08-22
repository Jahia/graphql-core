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
package org.jahia.modules.graphql.provider.dxm.service.tags;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

import java.util.Map.Entry;

/**
 * GraphQL representation of a Tag - generic implementation.
 */
@GraphQLName("GenericTagNode")
@GraphQLDescription("Generic tag node representation")
public class GqlTagNodeImpl implements GqlTagNode {

    private Entry<String, Long> node;

    /**
     * Create an instance that represents a tag.
     *
     * @param node A {@link java.util.Map.Entry} that contains the tag name as key and the occurences as value
     */
    public GqlTagNodeImpl(Entry<String, Long> node) {
        this.node = node;
    }

    @Override
    @GraphQLNonNull
    @GraphQLName("name")
    @GraphQLDescription("The tag name")
    public String getName() {
        return node.getKey();
    }

    @Override
    @GraphQLNonNull
    @GraphQLName("occurrences")
    @GraphQLDescription("Number of occurrences of this tag")
    public Long getOccurrences() {
        return node.getValue();
    }
}
