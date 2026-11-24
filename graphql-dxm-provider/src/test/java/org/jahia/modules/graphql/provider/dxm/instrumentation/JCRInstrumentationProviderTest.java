package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.versioning.VersionSessionInstrumentation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Covers the order of the instrumentations that the GraphQL provider runs.
 */
public class JCRInstrumentationProviderTest {

    @Test
    public void theVersionOfAQueryOpensBeforeTheIntrospectionCheckReadsASession() {
        JCRInstrumentationProvider provider = new JCRInstrumentationProvider();
        provider.bindDxGraphQLConfig(mock(DXGraphQLConfig.class));

        List<Instrumentation> chain = ((ChainedInstrumentation) provider.getInstrumentation()).getInstrumentations();

        int version = indexOf(chain, VersionSessionInstrumentation.class);
        int jcr = indexOf(chain, JCRInstrumentation.class);
        assertTrue("the chain holds both instrumentations", version >= 0 && jcr >= 0);
        assertTrue("the version opens before JCRInstrumentation", version < jcr);
    }

    private static int indexOf(List<Instrumentation> chain, Class<?> type) {
        for (int i = 0; i < chain.size(); i++) {
            if (type.isInstance(chain.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
