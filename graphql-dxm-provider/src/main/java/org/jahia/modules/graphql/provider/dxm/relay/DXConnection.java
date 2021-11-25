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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.relay.DefaultConnection;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;

import java.util.List;
import java.util.stream.Collectors;

public class DXConnection<T> extends DefaultConnection<T> {
    public DXConnection(List<Edge<T>> edges, PageInfo pageInfo) {
        super(edges, pageInfo);
    }

    public List<T> getNodes() {
        return getEdges().stream().map(Edge::getNode).collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("fieldAggregation")
    @GraphQLDescription("Get an aggregation by session data")
    public DXFieldAggregation<T> getFieldAggregation(DataFetchingEnvironment environment) {
        return new DXFieldAggregation<>(getEdges(), FieldEvaluator.forConnection((GraphQLOutputType) environment.getParentType(), environment));
    }
}
