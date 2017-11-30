package org.jahia.modules.graphql.provider.dxm.node;



import graphql.annotations.annotationTypes.GraphQLName;

import javax.jcr.PropertyType;

@GraphQLName("JCRPropertyType")
public enum GqlJcrPropertyType {
    BOOLEAN,
    DATE,
    DECIMAL,
    LONG,
    DOUBLE,
    BINARY,
    NAME,
    PATH,
    REFERENCE,
    STRING,
    UNDEFINED,
    URI,
    WEAKREFERENCE;

    public static GqlJcrPropertyType getValue(int type) {
        switch (type) {
            case PropertyType.STRING:
                return STRING;
            case PropertyType.BINARY:
                return BINARY;
            case PropertyType.BOOLEAN:
                return BOOLEAN;
            case PropertyType.LONG:
                return LONG;
            case PropertyType.DOUBLE:
                return DOUBLE;
            case PropertyType.DECIMAL:
                return DECIMAL;
            case PropertyType.DATE:
                return DATE;
            case PropertyType.NAME:
                return NAME;
            case PropertyType.PATH:
                return PATH;
            case PropertyType.REFERENCE:
                return REFERENCE;
            case PropertyType.WEAKREFERENCE:
                return WEAKREFERENCE;
            case PropertyType.URI:
                return URI;
            case PropertyType.UNDEFINED:
                return UNDEFINED;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }
}
