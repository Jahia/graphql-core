package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.io.IOUtils;
import org.jahia.modules.graphql.provider.dxm.scanner.FailingInitializerExtension;
import org.jahia.modules.graphql.provider.dxm.scanner.PlainExtension;
import org.jahia.modules.graphql.provider.dxm.scanner.ScannedProvider;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleScannerTest {

    private static final String PACKAGE = ScannedProvider.class.getPackage().getName();
    private static final String PACKAGE_PATH = PACKAGE.replace('.', '/');
    private static final String MALFORMED = PACKAGE + ".Malformed";

    @Test
    public void classWhoseStaticInitializerFailsIsScannedAndNotInitialized() throws Exception {
        assertEquals(asList(FailingInitializerExtension.class.getName(), PlainExtension.class.getName()),
                scannedExtensions("FailingInitializerExtension", "PlainExtension"));
    }

    @Test
    public void classThatCannotBeLinkedIsSkipped() throws Exception {
        assertEquals(asList(PlainExtension.class.getName()), scannedExtensions("Malformed", "PlainExtension"));
    }

    // Two scans of one bundle must return the same classes, also after the first use of a class
    @Test
    public void secondScanReturnsTheSameClassesAsTheFirst() throws Exception {
        DXGraphQLExtensionsProvider provider = provider("FailingInitializerExtension", "PlainExtension");

        assertEquals(names(provider.getExtensions()), names(provider.getExtensions()));
    }

    private static List<String> scannedExtensions(String... classNames) throws Exception {
        return names(provider(classNames).getExtensions());
    }

    private static List<String> names(Collection<Class<?>> classes) {
        return classes.stream().map(Class::getName).collect(Collectors.toList());
    }

    private static DXGraphQLExtensionsProvider provider(String... classNames) throws Exception {
        Bundle bundle = mock(Bundle.class);
        List<String> entries = asList(classNames).stream().map(name -> PACKAGE_PATH + "/" + name + ".class").collect(Collectors.toList());
        when(bundle.getEntryPaths(PACKAGE_PATH)).thenAnswer(invocation -> enumeration(entries));
        ClassLoader classLoader = new BundleClassLoader(bundle, BundleScannerTest.class.getClassLoader());
        return (DXGraphQLExtensionsProvider) classLoader.loadClass(ScannedProvider.class.getName()).getDeclaredConstructor().newInstance();
    }

    /**
     * Stands for the class loader of a bundle: BundleScanner reads its "bundle" property.
     * It defines the classes of the scanned package itself, so that each test starts with classes that are not initialized.
     */
    public static class BundleClassLoader extends ClassLoader {

        private final Bundle bundle;

        BundleClassLoader(Bundle bundle, ClassLoader parent) {
            super(parent);
            this.bundle = bundle;
        }

        public Bundle getBundle() {
            return bundle;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(PACKAGE + ".")) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                return loaded != null ? loaded : define(name);
            }
        }

        private Class<?> define(String name) throws ClassNotFoundException {
            byte[] bytes = MALFORMED.equals(name) ? new byte[]{0} : read(name);
            return defineClass(name, bytes, 0, bytes.length);
        }

        private byte[] read(String name) throws ClassNotFoundException {
            try (InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (stream == null) {
                    throw new ClassNotFoundException(name);
                }
                return IOUtils.toByteArray(stream);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
