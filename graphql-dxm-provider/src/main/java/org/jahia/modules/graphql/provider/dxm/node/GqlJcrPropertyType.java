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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;

import javax.jcr.PropertyType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@GraphQLName("JCRPropertyType")
@GraphQLDescription("JCR property type enumeration")
public enum GqlJcrPropertyType {
    @GraphQLDescription("Boolean property type")
    BOOLEAN(PropertyType.BOOLEAN),
    @GraphQLDescription("Date property type")
    DATE(PropertyType.DATE),
    @GraphQLDescription("Decimal property type")
    DECIMAL(PropertyType.DECIMAL),
    @GraphQLDescription("Long property type")
    LONG(PropertyType.LONG),
    @GraphQLDescription("Double property type")
    DOUBLE(PropertyType.DOUBLE),
    @GraphQLDescription("Binary property type")
    BINARY(PropertyType.BINARY),
    @GraphQLDescription("Name property type")
    NAME(PropertyType.NAME),
    @GraphQLDescription("Path property type")
    PATH(PropertyType.PATH),
    @GraphQLDescription("Reference property type")
    REFERENCE(PropertyType.REFERENCE),
    @GraphQLDescription("String property type")
    STRING(PropertyType.STRING),
    @GraphQLDescription("Undefined property type")
    UNDEFINED(PropertyType.UNDEFINED),
    @GraphQLDescription("URI property type")
    URI(PropertyType.URI),
    @GraphQLDescription("Weak reference property type")
    WEAKREFERENCE(PropertyType.WEAKREFERENCE);

    private int value;

    private static final Map<Integer,GqlJcrPropertyType> lookup = new HashMap<Integer,GqlJcrPropertyType>();

    static {
        for(GqlJcrPropertyType s : EnumSet.allOf(GqlJcrPropertyType.class))
            lookup.put(s.getValue(), s);
    }

    GqlJcrPropertyType(int value) {
        this.value = value;
    }

    public static GqlJcrPropertyType fromValue(int type) {
        if (lookup.containsKey(type)) {
            return lookup.get(type);
        }
        throw new IllegalArgumentException("unknown type: " + type);
    }

    public int getValue() {
        return value;
    }
}
