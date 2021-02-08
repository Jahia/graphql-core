package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.instrumentation.Instrumentation;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;

public interface JahiaInstrumentation {
    int getPriority();
    Instrumentation getInstrumentation(DXGraphQLConfig dxGraphQLConfig);
}
