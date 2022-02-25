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

import graphql.annotations.annotationTypes.GraphQLName;

import javax.jcr.PropertyType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@GraphQLName("JCRPropertyType")
public enum GqlJcrPropertyType {
    BOOLEAN(PropertyType.BOOLEAN),
    DATE(PropertyType.DATE),
    DECIMAL(PropertyType.DECIMAL),
    LONG(PropertyType.LONG),
    DOUBLE(PropertyType.DOUBLE),
    BINARY(PropertyType.BINARY),
    NAME(PropertyType.NAME),
    PATH(PropertyType.PATH),
    REFERENCE(PropertyType.REFERENCE),
    STRING(PropertyType.STRING),
    UNDEFINED(PropertyType.UNDEFINED),
    URI(PropertyType.URI),
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
