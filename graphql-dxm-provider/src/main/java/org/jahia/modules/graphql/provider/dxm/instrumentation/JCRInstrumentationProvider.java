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
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.kickstart.execution.config.InstrumentationProvider;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.config.GraphQLLimits;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.osgi.service.component.annotations.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JCR instrumentation provider, basic instrumentation provider that provide an instance of JCRInstrumentation
 */
@Component(immediate = true)
public class JCRInstrumentationProvider implements InstrumentationProvider {

    private DXGraphQLConfig dxGraphQLConfig;
    private List<JahiaInstrumentation> instrumentations = new ArrayList<>();

    @Reference
    public void bindDxGraphQLConfig(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Reference(service = JahiaInstrumentation.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, unbind = "unbindInstrumentation")
    public void bindInstrumentations(JahiaInstrumentation instrumentation) {
        this.instrumentations.add(instrumentation);
    }

    public void unbindInstrumentation(JahiaInstrumentation instrumentation) {
        instrumentations.remove(instrumentation);
    }

    @Override
    public Instrumentation getInstrumentation() {
        List<Instrumentation> instns = new ArrayList<>();

        // Query-cost guards: reject expensive documents before execution (i.e. before any field is fetched,
        // permission-checked or serialized). A value <= 0 disables the corresponding guard; with both disabled the
        // document is not analysed at all. One instrumentation covers the two of them because they share a single
        // traversal of the document - see QueryCostInstrumentation.
        int maxQueryComplexity = dxGraphQLConfig.getMaxQueryComplexity();
        int maxQueryDepth = dxGraphQLConfig.getMaxQueryDepth();
        // Read the batch bound from GraphQLLimits rather than the config directly, so that this guard and the per-field
        // check in the mutation resolvers can never disagree about the value in force. The node allowance is read from
        // PaginationHelper for the same reason: it is spent there, as the connections walk.
        int maxMutationBatchSize = GraphQLLimits.getMutationBatchLimit();
        int maxNodesPerRequest = PaginationHelper.getRequestNodeLimit();
        if (maxQueryComplexity > 0 || maxQueryDepth > 0 || maxMutationBatchSize > 0 || maxNodesPerRequest > 0) {
            instns.add(new QueryCostInstrumentation(maxQueryComplexity, maxQueryDepth, maxMutationBatchSize, maxNodesPerRequest));
        }

        instns.add(new JCRInstrumentation(dxGraphQLConfig));
        instns.addAll(instrumentations.stream()
                .sorted(Comparator.comparingInt(JahiaInstrumentation::getPriority))
                .map(inst -> inst.getInstrumentation(dxGraphQLConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return new ChainedInstrumentation(instns);
    }
}
