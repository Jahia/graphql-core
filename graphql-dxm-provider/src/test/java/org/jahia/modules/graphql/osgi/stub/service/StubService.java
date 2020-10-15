package org.jahia.modules.graphql.osgi.stub.service;

import org.mockito.invocation.InvocationOnMock;

/**
 * Stub OSGI Service used to store the invocation call on BundleUtils.getOsgiService
 * To be able to test and check that the invocation of BundleUtils.getOsgiService have been done using good parameters for service lookup.
 */
public class StubService {
    private final InvocationOnMock invocationOnMock;

    public StubService(InvocationOnMock invocationOnMock) {
        this.invocationOnMock = invocationOnMock;
    }

    public InvocationOnMock getInvocationOnMock() {
        return invocationOnMock;
    }
}
