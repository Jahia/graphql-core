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
package org.jahia.modules.graphql.provider.dxm.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wrap bean to access private fields/methods
 */
@SuppressWarnings("java:S3011")
public class BeanWrapper {
    private final Object bean;

    private BeanWrapper(Object bean) {
        this.bean = bean;
    }

    public static BeanWrapper wrap(Object bean) {
        return new BeanWrapper(bean);
    }

    public BeanWrapper get(String property) throws ReflectiveOperationException {
        return get(property, bean.getClass());
    }

    public BeanWrapper get(String property, Class<?> clazz) throws ReflectiveOperationException {
        Field f = clazz.getDeclaredField(property);
        f.setAccessible(true);
        return new BeanWrapper(f.get(bean));
    }

    public BeanWrapper call(String method) throws ReflectiveOperationException {
        return call(method, new Class[0], new Object[0]);
    }

    public BeanWrapper call(String method, Class<?> clazz) throws ReflectiveOperationException {
        return call(method, clazz, new Class[0], new Object[0]);
    }

    public BeanWrapper call(String method, Class<?>[] paramTypes, Object[] params) throws ReflectiveOperationException {
        return call(method, bean.getClass(), paramTypes, params);
    }

    public BeanWrapper call(String method, Class<?> clazz, Class<?>[] paramTypes, Object[] params) throws ReflectiveOperationException {
        Method m = clazz.getDeclaredMethod(method, paramTypes);
        m.setAccessible(true);
        return new BeanWrapper(m.invoke(bean, params));
    }

    public <T> T unwrap(Class<T> target) {
        return target.cast(bean);
    }
}
