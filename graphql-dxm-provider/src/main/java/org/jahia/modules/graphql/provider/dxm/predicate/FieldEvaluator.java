/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.TypeResolutionEnvironment;
import graphql.execution.FieldCollector;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.ValuesResolver;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.schema.*;
import graphql.servlet.GraphQLContext;

import java.util.List;
import java.util.Map;

import static graphql.schema.DataFetchingEnvironmentBuilder.newDataFetchingEnvironment;
import static org.jahia.modules.graphql.provider.dxm.instrumentation.JCRInstrumentation.FRAGMENTS_BY_NAME;
import static org.jahia.modules.graphql.provider.dxm.instrumentation.JCRInstrumentation.GRAPHQL_VARIABLES;

/**
 * Environment required to dynamically evaluate a sub-field
 */
public class FieldEvaluator {
    private GraphQLType type;
    private FieldFinder fieldFinder;
    private Map<String, Object> variables;
    private GraphQLContext context;

    private static FieldCollector fieldCollector = new FieldCollector();

    public FieldEvaluator(GraphQLType type, FieldFinder fieldFinder, Map<String, Object> variables, GraphQLContext context) {
        this.type = type;
        this.fieldFinder = fieldFinder;
        this.variables = variables;
        this.context = context;
    }

    @FunctionalInterface
    interface FieldFinder {
        Field find(GraphQLObjectType outputType, String name);
    }

    /**
     * Build an environment for a List type field
     * @param environment The DataFetchingEnvironment of the Connection field data fetcher
     * @return A FieldEvaluator instance
     */
    public static FieldEvaluator forList(DataFetchingEnvironment environment) {
        // Extract return type from connection
        GraphQLOutputType fieldType = environment.getFieldType();
        if (fieldType instanceof GraphQLNonNull) {
            fieldType = (GraphQLOutputType) ((GraphQLNonNull) fieldType).getWrappedType();
        }
        GraphQLList list = (GraphQLList)fieldType;
        GraphQLType type = list.getWrappedType();

        // Extract selection set
        FieldFinder fieldFinder = (objectType, name) -> {
            if (environment.getSelectionSet() != null) {
                return getField(environment.getSelectionSet().get(), name);
            }
            return null;
        };

        return new FieldEvaluator(type, fieldFinder, getVariables(environment), environment.getContext());
    }

    /**
     * Build an environment for a Connection type field
     * @param environment The DataFetchingEnvironment of the Connection field data fetcher
     * @return A FieldEvaluator instance
     */
    public static FieldEvaluator forConnection(DataFetchingEnvironment environment) {
        Map<String, Object> variables = getVariables(environment);

        // Extract return type from connection
        GraphQLOutputType fieldType = environment.getFieldType();
        if (fieldType instanceof GraphQLNonNull) {
            fieldType = (GraphQLOutputType) ((GraphQLNonNull) fieldType).getWrappedType();
        }
        GraphQLObjectType outputType = ((GraphQLObjectType) fieldType);
        GraphQLList list = (GraphQLList) outputType.getFieldDefinition("nodes").getType();
        GraphQLType type = list.getWrappedType();

        FieldFinder fieldFinder = (objectType, name) -> {
            FieldCollectorParameters parameters = FieldCollectorParameters.newParameters()
                    .objectType(objectType)
                    .variables(variables)
                    .fragments(getFragmentDefinitions(environment))
                    .schema(environment.getGraphQLSchema())
                    .build();

            // Extract selection set on "{ nodes }" or "{ edges { node } }"
            Map<String, List<Field>> fields = null;
            if (environment.getSelectionSet() != null) {
                fields = environment.getSelectionSet().get();
                Field returnedField = null;
                if (fields.containsKey("nodes")) {
                    // First look in { nodes } selection set
                    List<Field> nodeFields = fields.get("nodes");
                    returnedField = getField(fieldCollector.collectFields(parameters, nodeFields), name);
                } else if (fields.containsKey("edges")) {
                    // If no "nodes" was found, try to look into { edges { node } } selection set
                    List<Field> edgeFields = fields.get("edges");
                    fields = fieldCollector.collectFields(parameters, edgeFields);
                    if (fields.containsKey("node")) {
                        returnedField = getField(fieldCollector.collectFields(parameters, fields.get("node")), name);
                    }
                }
            }
            return null;
        };

        return new FieldEvaluator(type, fieldFinder, variables, environment.getContext());
    }

    private static Map<String, Object> getVariables(DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getContext();
        Map<String, Object> variables = null;
        if (context.getRequest().isPresent()) {
            variables = (Map<String, Object>) context.getRequest().get().getAttribute(GRAPHQL_VARIABLES);
        }
        return variables;
    }

    private static Map<String, FragmentDefinition> getFragmentDefinitions(DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getContext();
        Map<String, FragmentDefinition> fragments = null;
        if (context.getRequest().isPresent()) {
            fragments = (Map<String, FragmentDefinition>) context.getRequest().get().getAttribute(FRAGMENTS_BY_NAME);
        }
        return fragments;
    }

    private static Field getField(Map<String, List<Field>> fields, String name) {
        if (fields != null && fields.containsKey(name)) {
            return fields.get(name).get(0);
        }
        return null;
    }

    /**
     * @return The GraphQLObject type which is returned by the field
     */
    private GraphQLObjectType getObjectType(Object object) {
        if (type instanceof GraphQLObjectType) {
            return (GraphQLObjectType) type;
        } else if (type instanceof GraphQLInterfaceType) {
            return ((GraphQLInterfaceType) type).getTypeResolver().getType(new TypeResolutionEnvironment(object, null, null, null, null, null));
        } else {
            return null;
        }
    }

    /**
     * Evaluate a field value on a given object
     *
     * @param source      The source object on which we will get the field value
     * @param fieldName   The field name or alias
     * @return The value, as returned by the DataFetcher
     */
    public Object getFieldValue(Object source, String fieldName) {
        GraphQLObjectType objectType = getObjectType(source);
        if (objectType == null) {
            return null;
        }

        DataFetchingEnvironmentBuilder fieldEnv = newDataFetchingEnvironment().source(source).context(context);

        // Try to find field in selection set to reuse alias/arguments
        Field field = fieldFinder.find(objectType, fieldName);
        if (field != null) {
            GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition(field.getName());
            if (fieldDefinition == null) {
                // Definition not present on current type (can be a field in a non-matching fragment), returns null
                return null;
            }

            ValuesResolver valuesResolver = new ValuesResolver();
            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDefinition.getArguments(), field.getArguments(), variables);
            fieldEnv.arguments(argumentValues);
            fieldEnv.fieldType(fieldDefinition.getType());
            return fieldDefinition.getDataFetcher().get(fieldEnv.build());
        }

        // Otherwise, directly look in field definitions
        GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition(fieldName);
        if (fieldDefinition == null) {
            // Definition not present on current type (can be a field in a non-matching fragment), returns null
            return null;
        }
        return fieldDefinition.getDataFetcher().get(fieldEnv.build());
    }
}
