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
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.List;

import static graphql.Scalars.GraphQLString;

public class ByIdFinderDataFetcher extends FinderBaseDataFetcher {

    public ByIdFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> defaultArguments = getDefaultArguments();
        defaultArguments.add(GraphQLArgument.newArgument().name("id").description("Node identifier").type(GraphQLString).build());
        return defaultArguments;
    }

    @Override
    public GqlJcrNode get(DataFetchingEnvironment environment) {
        try {
            return new GqlJcrNodeImpl(getCurrentUserSession(environment).getNodeByIdentifier((String) SDLUtil.getArgument("id", environment)));
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
