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
package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderBaseDataFetcher;

import java.util.List;

public class FinderAdapter implements DataFetcher {

    private FinderBaseDataFetcher originalFinder;
    private List<FinderMixinInterface> applicableFinderMixins;

    public FinderAdapter(FinderBaseDataFetcher originalFinder, List<FinderMixinInterface> applicableFinderMixins) {
        this.originalFinder = originalFinder;
        this.applicableFinderMixins = applicableFinderMixins;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (originalFinder == null) {
            return null;
        }

        Object originalFinderResult = originalFinder.get(environment);

        for (FinderMixinInterface mixin : applicableFinderMixins) {
            originalFinderResult = mixin.resolveNode((GqlJcrNode) originalFinderResult, environment);
            if (originalFinderResult == null) {
                return null;
            }
        }

        return originalFinderResult;
    }
}
