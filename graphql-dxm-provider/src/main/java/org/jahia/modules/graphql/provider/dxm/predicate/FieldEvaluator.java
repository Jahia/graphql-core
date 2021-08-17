/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.TypeResolutionEnvironment;
import graphql.execution.*;
import graphql.language.FragmentDefinition;
import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.osgi.OSGIServiceInjectorDataFetcher;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static org.jahia.modules.graphql.provider.dxm.instrumentation.JCRInstrumentation.FRAGMENTS_BY_NAME;
import static org.jahia.modules.graphql.provider.dxm.instrumentation.JCRInstrumentation.GRAPHQL_VARIABLES;

/**
 * Environment required to dynamically evaluate a sub-field
 */
public class FieldEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(FieldEvaluator.class);

    private GraphQLType type;
    private FieldFinder fieldFinder;
    private Map<String, Object> variables;
    private DataFetchingEnvironment environment;

    private static final String FIELD_NAME_SEPARATOR_REGEX = "\\.";

    private static FieldCollector fieldCollector = new FieldCollector();

    private FieldEvaluator(GraphQLType type, FieldFinder fieldFinder, Map<String, Object> variables, DataFetchingEnvironment environment) {
        this.type = type;
        this.fieldFinder = fieldFinder;
        this.variables = variables;
        this.environment = environment;
    }

    @FunctionalInterface
    interface FieldFinder {
        SelectedField find(GraphQLObjectType outputType, String name);
    }

    /**
     * Build an environment for a List type field
     *
     * @param environment The DataFetchingEnvironment of the Connection field data fetcher
     * @return A FieldEvaluator instance
     */
    public static FieldEvaluator forList(DataFetchingEnvironment environment) {
        // Extract return type from connection
        GraphQLOutputType fieldType = environment.getFieldType();
        if (fieldType instanceof GraphQLNonNull) {
            fieldType = (GraphQLOutputType) ((GraphQLNonNull) fieldType).getWrappedType();
        }
        GraphQLList list = (GraphQLList) fieldType;
        GraphQLType type = list.getWrappedType();

        // Extract selection set
        FieldFinder fieldFinder = (objectType, name) -> {
            if (environment.getSelectionSet() != null) {
                return getField(environment.getSelectionSet().getFieldsGroupedByResultKey(), name);
            }
            return null;
        };

        return new FieldEvaluator(type, fieldFinder, getVariables(environment), environment);
    }

    /**
     * Build an environment for a Connection type field
     *
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
            if (environment.getSelectionSet() != null) {
                DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet();
                if (selectionSet.contains("nodes")) {
                    return getField(selectionSet.getFieldsGroupedByResultKey("nodes/**"), name);
                } else if (selectionSet.contains("edges/node/*")) {
                    return getField(selectionSet.getFieldsGroupedByResultKey("edges/node/**"), name);
                }
            }
            return null;
        };

        return new FieldEvaluator(type, fieldFinder, variables, environment);
    }

    private static Map<String, Object> getVariables(DataFetchingEnvironment environment) {
        HttpServletRequest request = ContextUtil.getHttpServletRequest(environment.getContext());
        return (request != null) ?
                (Map<String, Object>) request.getAttribute(GRAPHQL_VARIABLES) :
                new LinkedHashMap<>();
    }

    private static Map<String, FragmentDefinition> getFragmentDefinitions(DataFetchingEnvironment environment) {
        HttpServletRequest request = ContextUtil.getHttpServletRequest(environment.getContext());
        return (request != null) ?
                (Map<String, FragmentDefinition>) request.getAttribute(FRAGMENTS_BY_NAME) :
                new LinkedHashMap<>();
    }

    private static SelectedField getField(Map<String, List<SelectedField>> fieldsByKey, String name) {
        if (fieldsByKey != null && fieldsByKey.containsKey(name)) {
            return fieldsByKey.get(name).stream()
                    .filter(f -> name.equals(f.getName()))
                    .findFirst()
                    .orElse(null);
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
            TypeResolver typeResolver = environment.getGraphQLSchema().getCodeRegistry().getTypeResolver((GraphQLInterfaceType) type);
            return typeResolver.getType(new TypeResolutionEnvironment(object, null, null, null, null, null));
        } else {
            return null;
        }
    }

    /**
     * Evaluate a field value on a given object
     *
     * @param source    The source object on which we will get the field value
     * @param fieldName The field name or alias
     * @return The value, as returned by the DataFetcher
     */
    public Object getFieldValue(Object source, String fieldName) {
        String[] splitFields = fieldName.split(FIELD_NAME_SEPARATOR_REGEX, 2);
        String nextField = null;
        if (splitFields.length > 1) {
            fieldName = splitFields[0];
            nextField = splitFields[1];
        }

        GraphQLObjectType objectType = getObjectType(source);
        if (objectType == null) {
            return null;
        }

        try {
            // Inject object field if necessary
            OSGIServiceInjectorDataFetcher.handleMethodInjection(source);
        } catch (IllegalAccessException |InvocationTargetException e) {
            logger.warn("Cannot inject fields ",e);
        }

        DataFetchingEnvironmentImpl.Builder fieldEnvBuilder = newDataFetchingEnvironment()
                .source(source)
                .parentType(objectType)
                .context(environment.getContext())
                .root(environment.getRoot())
                .executionId(environment.getExecutionId())
                .fragmentsByName(environment.getFragmentsByName())
                .graphQLSchema(environment.getGraphQLSchema())
                .arguments(environment.getArguments())
                .executionStepInfo(environment.getExecutionStepInfo());

        // Try to find field in selection set to reuse alias/arguments
        SelectedField field = fieldFinder.find(objectType, fieldName);
        GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition((field != null) ? field.getName() : fieldName);

        if (fieldDefinition == null) {
            // Definition not present on current type (can be a field in a non-matching fragment), returns null
            return null;
        }
        fieldEnvBuilder.fieldDefinition(fieldDefinition);
        fieldEnvBuilder.fieldType(fieldDefinition.getType());
        fieldEnvBuilder.executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                .fieldDefinition(fieldDefinition)
                .type(fieldDefinition.getType())
                .build());

        Object value = null;
        try {
            DataFetchingEnvironment fieldEnv = fieldEnvBuilder.build();
            value = environment.getGraphQLSchema().getCodeRegistry().getDataFetcher(objectType, fieldDefinition).get(fieldEnv);
        } catch (Exception e) {
            value = e.getMessage();
        }

        if (nextField != null && value != null) {
            return forSubField(fieldDefinition.getType(), field != null ? field.getSelectionSet() : null).getFieldValue(value, nextField);
        } else {
            return value;
        }
    }

    private FieldEvaluator forSubField(GraphQLOutputType fieldType, DataFetchingFieldSelectionSet selectionSet) {
        if (fieldType instanceof GraphQLNonNull) {
            fieldType = (GraphQLOutputType) ((GraphQLNonNull) fieldType).getWrappedType();
        }

        // Extract selection set
        FieldFinder fieldFinder = (objectType, name) -> {
            if (selectionSet != null) {
                FieldCollectorParameters parameters = FieldCollectorParameters.newParameters()
                        .objectType(objectType)
                        .variables(getVariables(environment))
                        .fragments(getFragmentDefinitions(environment))
                        .schema(environment.getGraphQLSchema())
                        .build();

                return getField(selectionSet.getFieldsGroupedByResultKey(), name);
            }
            return null;
        };

        return new FieldEvaluator(fieldType, fieldFinder, getVariables(environment), environment);
    }


}
