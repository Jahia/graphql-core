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
package org.jahia.modules.graphql.provider.dxm;

import org.apache.commons.beanutils.PropertyUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class BundleScanner {
    private static Logger logger = LoggerFactory.getLogger(BundleScanner.class);

    public static <T> Collection<Class<? extends T>> getClasses(Object context, Class<? extends Annotation> annotation) {
        List<Class<? extends T>> classes = new ArrayList<>();
        String packageName = context.getClass().getPackage().getName();
        String path = packageName.replace('.', '/');
        findClasses(path, annotation, classes, context.getClass().getClassLoader());

        return classes;
    }

    @SuppressWarnings("unchecked")
    private static <T> void findClasses(String path, Class<? extends Annotation> annotation, List<Class<? extends T>> classes, ClassLoader classLoader) {
        try {
            Bundle bundle = (Bundle) PropertyUtils.getProperty(classLoader, "bundle");
            Enumeration<String> items = bundle.getEntryPaths(path);
            if (items != null) {
                while (items.hasMoreElements()) {
                    String subpath = items.nextElement();
                    if (subpath.endsWith(".class")) {
                        if (!subpath.contains("$")) {
                            try {
                                Class<?> c = Class.forName(subpath.substring(0, subpath.length() - 6).replace('/', '.'), true, classLoader);
                                if (c.isAnnotationPresent(annotation)) {
                                    classes.add((Class<? extends T>) c);
                                }
                            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                                logger.warn("Cannot load class " + subpath);
                            }
                        }
                    } else {
                        findClasses(subpath, annotation, classes, classLoader);
                    }
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logger.warn("Cannot scan classes from " + classLoader + " classpath", e);
        }
    }
}
