/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm;

import graphql.schema.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Merge
 */
public class CodeRegistryFetcher {
    private final FieldCoordinates coords;
    private final GraphQLFieldDefinition fieldDef;

    CodeRegistryFetcher(GraphQLObjectType objectType, GraphQLFieldDefinition fieldDef) {
        coords = FieldCoordinates.coordinates(objectType, fieldDef);
        this.fieldDef = fieldDef;
    }

    public DataFetcher<?> getDataFetcher(GraphQLSchema schema) {
        return schema.getCodeRegistry().getDataFetcher(coords, fieldDef);
    }

    public FieldCoordinates getCoords() {
        return coords;
    }

    public static GraphQLCodeRegistry fromSchema(GraphQLSchema schema) {
        GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();
        List<String> reservedTypes = Arrays.asList("Query", "Mutation", "Subscription", "__");
        schema.getAllTypesAsList().stream()
                .filter(type -> type instanceof GraphQLObjectType && !reservedTypes.stream().anyMatch(rt -> type.getName().startsWith(rt)))
                .map(objType -> ((GraphQLObjectType) objType))
                .flatMap(objType -> objType.getFieldDefinitions().stream().map(field -> new CodeRegistryFetcher(objType, field)))
                .forEach(coords -> builder.dataFetcher(coords.getCoords(), coords.getDataFetcher(schema)));
        return builder.build();
    }

}
