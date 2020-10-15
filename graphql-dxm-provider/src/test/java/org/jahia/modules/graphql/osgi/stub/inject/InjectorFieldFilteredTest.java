package org.jahia.modules.graphql.osgi.stub.inject;

import org.jahia.modules.graphql.osgi.stub.service.StubService;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;

public class InjectorFieldFilteredTest implements InjectorTest {

    @Inject
    @GraphQLOsgiService(filter = "(port=8080)")
    private StubService stubService;

    public StubService getStubService() {
        return stubService;
    }
}
