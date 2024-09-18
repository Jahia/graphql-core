/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.osgi;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.osgi.BundleUtils;
import pl.touk.throwing.ThrowingConsumer;
import pl.touk.throwing.ThrowingFunction;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
    @SuppressWarnings("unchecked")
    public T get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        T data = originalDataFetcher.get(dataFetchingEnvironment);

        if (data instanceof CompletableFuture<?>) {
            return (T) ((CompletableFuture<Object>) data).thenApply((Function<Object, Object>) ThrowingFunction.unchecked(this::inject));
        } else {
            return inject(data);
        }
    }

    private T inject(T data) throws IllegalAccessException, InvocationTargetException {
        if (data instanceof DataFetcherResult && ((DataFetcherResult) data).getData() instanceof Collection) {
            for (Object item : ((Collection) ((DataFetcherResult) data).getData())) {
                handleMethodInjection(item);
            }
        }

        if (data instanceof Collection) {
            for (Object item : ((Collection) data)) {
                handleMethodInjection(item);
            }
        } else if (data != null) {
            handleMethodInjection(data);
        }

        return data;
    }

    public static void handleMethodInjection(Object data) throws IllegalAccessException, InvocationTargetException {
        for (Method method : MethodUtils.getMethodsListWithAnnotation(data.getClass(), Inject.class, true, true)) {
            handleMethodInjection(data, method);
        }

        for (Field field : FieldUtils.getFieldsListWithAnnotation(data.getClass(), Inject.class)) {
            handleFieldInjection(data, field);
        }
    }

    private static void handleMethodInjection(Object data, Method method) throws IllegalAccessException, InvocationTargetException {
        if (method.isAnnotationPresent(GraphQLOsgiService.class) && method.getParameterTypes().length > 0) {

            GraphQLOsgiService annotation = method.getAnnotation(GraphQLOsgiService.class);
            Class<?> klass = annotation.service().equals(Object.class) ? method.getParameterTypes()[0] : annotation.service();
            String filter = StringUtils.isNotEmpty(annotation.filter()) ? annotation.filter() : null;

            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            method.invoke(data, BundleUtils.getOsgiService(klass, filter));
        }
    }

    private static void handleFieldInjection(Object data, Field field) throws IllegalAccessException {
        if (field.isAnnotationPresent(GraphQLOsgiService.class)) {

            GraphQLOsgiService annotation = field.getAnnotation(GraphQLOsgiService.class);
            Class<?> klass = annotation.service().equals(Object.class) ? field.getType() : annotation.service();
            String filter = StringUtils.isNotEmpty(annotation.filter()) ? annotation.filter() : null;

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            field.set(data, BundleUtils.getOsgiService(klass, filter));
        }
    }
}
