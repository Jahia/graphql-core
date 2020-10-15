package org.jahia.modules.graphql.osgi;

import org.jahia.modules.graphql.osgi.stub.inject.*;
import org.jahia.modules.graphql.osgi.stub.service.StubService;
import org.jahia.modules.graphql.osgi.stub.service.StubServiceSubClass;
import org.jahia.modules.graphql.provider.dxm.osgi.OSGIServiceInjectorDataFetcher;
import org.jahia.osgi.BundleUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.jahia.osgi.BundleUtils.getOsgiService;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BundleUtils.class)
public class OsgiServiceInjectorTest {

    /**
     * Should inject service when using annotation on Method
     */
    @Test
    public void testMethodInjector() throws Exception {
        doInjectionTest(new InjectorMethodTest(), StubService.class, null);
    }

    /**
     * Should inject service when using annotation on private Method
     */
    @Test
    public void testMethodPrivateInjector() throws Exception {
        doInjectionTest(new InjectorMethodPrivateTest(), StubService.class, null);
    }

    /**
     * Should inject service when using annotation in inherited classe Method
     */
    @Test
    public void testMethodInheritedInjector() throws Exception {
        doInjectionTest(new InjectorMethodInheritedTest(), StubService.class, null);
    }

    /**
     * Should inject service when using annotation on Method with configured type
     */
    @Test
    public void testMethodTypedInjector() throws Exception {
        doInjectionTest(new InjectorMethodTypedTest(), StubServiceSubClass.class, null);
    }

    /**
     * Should inject service when using annotation on Method with configured filter
     */
    @Test
    public void testMethodFilteredInjector() throws Exception {
        doInjectionTest(new InjectorMethodFilteredTest(), StubService.class, "(port=8080)");
    }

    /**
     * Should inject service when using annotation on Field
     */
    @Test
    public void testFieldInjector() throws Exception {
        doInjectionTest(new InjectorFieldTest(), StubService.class, null);
    }

    /**
     * Should inject service when using annotation on Field with a configured type
     */
    @Test
    public void testFieldTypedInjector() throws Exception {
        doInjectionTest(new InjectorFieldTypedTest(), StubServiceSubClass.class, null);
    }

    /**
     * Should inject service when using annotation on Field in super classes
     */
    @Test
    public void testFieldInheritedInjector() throws Exception {
        doInjectionTest(new InjectorFieldInheritedTest(), StubService.class, null);
    }

    /**
     * Should inject service when using annotation on Field with a configured filter
     */
    @Test
    public void testFieldFilteredInjector() throws Exception {
        doInjectionTest(new InjectorFieldFilteredTest(), StubService.class, "(port=8080)");
    }

    private void doInjectionTest(InjectorTest injectorTest, Class<?> expectedClass, String expectedFilter) throws Exception {
        // Prepare Mock for BundleUtils.getOsgiService(..., ...)
        mockStatic(BundleUtils.class);
        Class<StubService> klass = Mockito.any();
        when(getOsgiService(klass, Mockito.any()))
                .then(StubService::new);

        // Build a data fetcher for testing (just returning the injectorTest as it is)
        OSGIServiceInjectorDataFetcher<?> dataFetcher = new OSGIServiceInjectorDataFetcher<>(dataFetchingEnvironment -> injectorTest);

        // perform .get() that will apply injection logic
        InjectorTest injectorMethodTest = (InjectorTest) dataFetcher.get(null);

        // Check resulted object contains correctly the injected service.
        Assert.assertEquals(expectedClass, injectorMethodTest.getStubService().getInvocationOnMock().getArgument(0));
        Assert.assertEquals(expectedFilter, injectorMethodTest.getStubService().getInvocationOnMock().getArgument(1));
    }
}
