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
package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedType;
import org.jahia.modules.graphql.provider.dxm.sdl.extension.FinderMixinInterface;
import org.jahia.modules.graphql.provider.dxm.sdl.extension.PropertyFetcherExtensionInterface;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DXGraphQLExtensionsProvider {

    default Collection<Class<?>> getExtensions() {
        return BundleScanner.getClasses(this, GraphQLTypeExtension.class);
    }

    default Collection<Class<? extends GqlJcrNode>> getSpecializedTypes() {
        return BundleScanner.getClasses(this, SpecializedType.class);
    }

    // Makes it possible to add functionality to finders where GqlJCRNode from original finder is passed to the one defined as mixin
    default List<FinderMixinInterface> getFinderMixins() {
        return Collections.emptyList();
    }

    // Makes it possible to add custom property fetchers, works together with @mapping(fetcher: "yourFetcherName")
    default Map<String, PropertyFetcherExtensionInterface> getPropertyFetchers() {
        return Collections.emptyMap();
    }
}
