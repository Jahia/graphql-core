package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLName;

import javax.jcr.Property;
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
