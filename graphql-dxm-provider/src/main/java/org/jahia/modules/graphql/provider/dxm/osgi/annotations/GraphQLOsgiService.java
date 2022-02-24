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
package org.jahia.modules.graphql.provider.dxm.osgi.annotations;

import org.jahia.modules.graphql.provider.dxm.osgi.OSGIServiceInjectorDataFetcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify the annotated member as a reference of an OSGI Service.
 *
 * <p>
 * When the annotation is applied to a method, the method is the bind method of
 * the reference. When the annotation is applied to a field, the field will
 * contain the bound service(s) of the reference.
 *
 * <p>
 * This annotation is processed at runtime by {@link OSGIServiceInjectorDataFetcher}.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLOsgiService {

    /**
     * The type of the service for this reference.
     *
     * <p>
     * If not specified, the type of the service for this reference is based
     * upon how this annotation is used:
     * <ul>
     * <li>Annotated method - The type of the service is the type of the first
     * argument of the method.</li>
     * <li>Annotated field - The type of the service is the
     * type of the field.</li>
     * </ul>
     */
    Class<?> service() default Object.class;

    /**
     * The OSGI filter for this reference.
     *
     * <p>
     * If not specified, no filter is used for service lookup.
     */
    String filter() default "";
}