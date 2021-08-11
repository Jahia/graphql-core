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
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.connection.ConnectionFetcher;
import graphql.relay.*;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DXPaginatedDataConnectionFetcher<T> implements ConnectionFetcher<T> {

    private DataFetcher<DXPaginatedData<T>> paginationDataFetcher;

    public DXPaginatedDataConnectionFetcher(DataFetcher<DXPaginatedData<T>> paginationDataFetcher) {
        this.paginationDataFetcher = paginationDataFetcher;
    }

    @Override
    public Connection<T> get(DataFetchingEnvironment environment) throws Exception {
        DXPaginatedData<T> paginatedData = paginationDataFetcher.get(environment);
        if (paginatedData == null) {
            return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null,null,false,false));
        }
        List<Edge<T>> edges = buildEdges(paginatedData);
        PageInfo pageInfo = getPageInfo(edges, paginatedData);

        GraphQLType fieldType = environment.getExecutionStepInfo().getFieldDefinition().getType();
        Class<? extends DXConnection<T>> connectionType =
                (Class<? extends DXConnection<T>>) DXGraphQLProvider.getInstance().getConnectionType(GqlTypeUtil.getTypeName(fieldType));
        if (connectionType != null) {
            try {
                return connectionType.getConstructor(List.class, PageInfo.class).newInstance(edges, pageInfo);
            } catch (ReflectiveOperationException e) {
                throw new DataFetchingException(e);
            }
        }
        return new DXConnection<T>(edges, pageInfo);
    }

    private DXPageInfo getPageInfo(List<Edge<T>> edges, DXPaginatedData<T> paginatedData) {
        return new DXPageInfo(
                edges.size() > 0 ? edges.get(0).getCursor() : null,
                edges.size() > 0 ? edges.get(edges.size() - 1).getCursor() : null,
                paginatedData);
    }

    private List<Edge<T>> buildEdges(DXPaginatedData<T> paginatedData) {
        Iterator<T> data = paginatedData.iterator();
        List<Edge<T>> edges = new ArrayList<>();
        for (; data.hasNext(); ) {
            T entity = data.next();
            edges.add(new DXEdge<>(entity, paginatedData.getIndex(entity), new DefaultConnectionCursor(paginatedData.getCursor(entity))));
        }
        return edges;
    }

}
