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
        instns.add(new JCRInstrumentation(dxGraphQLConfig));
        instns.add(new JCRTrackerInstrumentation());
        instns.addAll(instrumentations.stream()
                .sorted(Comparator.comparingInt(JahiaInstrumentation::getPriority))
                .map(inst -> inst.getInstrumentation(dxGraphQLConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return new ChainedInstrumentation(instns);
    }
}
