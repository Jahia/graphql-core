/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.connection.ConnectionFetcher;
import graphql.relay.*;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;

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
    public Connection<T> get(DataFetchingEnvironment environment) {
        DXPaginatedData<T> paginatedData = paginationDataFetcher.get(environment);
        if (paginatedData == null) {
            return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null,null,false,false));
        }
        List<Edge<T>> edges = buildEdges(paginatedData);
        PageInfo pageInfo = getPageInfo(edges, paginatedData);
        Class<? extends DXConnection<T>> connectionType = (Class<? extends DXConnection<T>>) DXGraphQLProvider.getInstance().getConnectionType(environment.getFieldTypeInfo().getType().getName());
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
                paginatedData.hasPreviousPage(),
                paginatedData.hasNextPage(),
                paginatedData.getNodesCount(),
                paginatedData.getTotalCount());
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
