package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.instrumentation.Instrumentation;
import graphql.servlet.InstrumentationProvider;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * JCR instrumentation provider, basic instrumentation provider that provide an instance of JCRInstrumentation
 */
@Component(immediate = true)
public class JCRInstrumentationProvider implements InstrumentationProvider {

    private DXGraphQLConfig dxGraphQLConfig;

    @Reference
    public void bindDxGraphQLConfig(DXGraphQLConfig dxGraphQLConfig) {
        this.dxGraphQLConfig = dxGraphQLConfig;
    }

    @Override
    public Instrumentation getInstrumentation() {
        return new JCRInstrumentation(dxGraphQLConfig);
    }
}
