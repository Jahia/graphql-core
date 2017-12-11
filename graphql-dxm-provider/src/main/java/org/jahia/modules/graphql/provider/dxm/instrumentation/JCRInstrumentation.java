package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.security.GqlJcrPermissionDataFetcher;

/**
 * JCR instrumentation implementation
 */
public class JCRInstrumentation extends NoOpInstrumentation {

    DXGraphQLConfig dxGraphQLConfig;

    JCRInstrumentation(DXGraphQLConfig dxGraphQLConfig) {
        super();
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        return super.instrumentDataFetcher(new GqlJcrPermissionDataFetcher<>(dataFetcher, dxGraphQLConfig.getPermissions()), parameters);
    }
}
