package org.jahia.modules.graphql.osgi.stub.inject;

import org.jahia.modules.graphql.osgi.stub.service.StubService;
import org.jahia.modules.graphql.osgi.stub.service.StubServiceSubClass;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;

public class InjectorMethodTypedTest implements InjectorTest {
    private StubService stubService;

    public StubService getStubService() {
        return stubService;
    }

    @Inject
    @GraphQLOsgiService(service = StubServiceSubClass.class)
    public void setStubService(StubService stubService) {
        this.stubService = stubService;
    }
}
