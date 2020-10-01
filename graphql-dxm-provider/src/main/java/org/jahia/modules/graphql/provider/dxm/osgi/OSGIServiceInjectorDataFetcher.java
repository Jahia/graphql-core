package org.jahia.modules.graphql.provider.dxm.osgi;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.osgi.BundleUtils;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This custom data fetcher is used to detect if the returned data class need some OSGI Service injection
 *
 * If the returned object contains {@link Inject} annotated methods, the OSGIServiceInjectorDataFetcher will call this methods
 * with resolved parameters as OSGI services.
 *
 * @param <T> type of the returned value
 */
public class OSGIServiceInjectorDataFetcher<T> implements DataFetcher<T> {
    private final DataFetcher<T> originalDataFetcher;

    public OSGIServiceInjectorDataFetcher(DataFetcher<T> originalDataFetcher) {
        this.originalDataFetcher = originalDataFetcher;
    }

    @Override
    public T get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        T data = originalDataFetcher.get(dataFetchingEnvironment);

        if (data != null) {
            for (Method method : annotatedMethodLookup(data.getClass(), Inject.class)) {
                method.invoke(data, Arrays.stream(method.getParameterTypes())
                        .map(klass -> BundleUtils.getOsgiService(klass, null))
                        .toArray(Object[]::new));
            }
        }

        return data;
    }

    private static List<Method> annotatedMethodLookup(final Class<?> type, final Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();
        Class<?> klass = type;

        // breaking clause to stop at Object Class, because it's the higher super class
        while (klass != Object.class) {
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation) && method.isAccessible()) {
                    methods.add(method);
                }
            }
            // check parents classes
            klass = klass.getSuperclass();
        }
        return methods;
    }
}
