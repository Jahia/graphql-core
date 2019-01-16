/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
                            } catch (ClassNotFoundException e) {
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
