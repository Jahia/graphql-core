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

import graphql.annotations.connection.AbstractPaginatedData;

public abstract class AbstractDXPaginatedData<T> extends AbstractPaginatedData<T> implements DXPaginatedData<T> {
    protected int nodesCount;
    protected int totalCount;

    public AbstractDXPaginatedData(Iterable<T> data, boolean hasPreviousPage, boolean hasNextPage, int nodesCount, int totalCount) {
        super(hasPreviousPage, hasNextPage, data);
        this.nodesCount = nodesCount;
        this.totalCount = totalCount;
    }

    public int getNodesCount() {
        return nodesCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
