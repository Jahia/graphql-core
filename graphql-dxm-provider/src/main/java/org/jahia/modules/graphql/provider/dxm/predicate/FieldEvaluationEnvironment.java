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
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.*;
import graphql.servlet.GraphQLContext;

import java.util.List;
import java.util.Map;

import static org.jahia.modules.graphql.provider.dxm.instrumentation.JCRInstrumentation.GRAPHQL_VARIABLES;

/**
 * Environment required to dynamically evaluate a sub-field
 */
public class FieldEvaluationEnvironment {
    private GraphQLType type;
    private SelectionSet selectionSet;
    private Map<String, Object> variables;

    public FieldEvaluationEnvironment(GraphQLType objectType, SelectionSet selectionSet, Map<String, Object> variables) {
        this.type = objectType;
        this.selectionSet = selectionSet;
        this.variables = variables;
    }

    /**
     * Build an environment for a List type field
     * @param environment The DataFetchingEnvironment of the Connection field data fetcher
     * @return A FieldEvaluationEnvironment instance
     */
    public static FieldEvaluationEnvironment buildEnvironmentForList(DataFetchingEnvironment environment) {
        // Extract return type from connection
        GraphQLList list =  ((GraphQLList)environment.getFieldType());
        GraphQLType type = list.getWrappedType();

        // Extract selection set
        SelectionSet selectionSet = null;
        if (environment.getSelectionSet() != null) {
            // Todo
        }

        return new FieldEvaluationEnvironment(type, selectionSet, getVariables(environment));
    }

    /**
     * Build an environment for a Connection type field
     * @param environment The DataFetchingEnvironment of the Connection field data fetcher
     * @return A FieldEvaluationEnvironment instance
     */
    public static FieldEvaluationEnvironment buildEnvironmentForConnection(DataFetchingEnvironment environment) {
        // Extract return type from connection
        GraphQLObjectType outputType = ((GraphQLObjectType)environment.getFieldType());
        GraphQLList list = (GraphQLList) outputType.getFieldDefinition("nodes").getType();
        GraphQLType type = list.getWrappedType();

        // Extract selection set on "{ nodes }" or "{ edges { node } }"
        SelectionSet selectionSet = null;
        if (environment.getSelectionSet() != null) {
            // First look in { nodes } selection set
            List<Field> nodeFields = environment.getSelectionSet().get().get("nodes");
            if (nodeFields != null) {
                selectionSet = nodeFields.get(0).getSelectionSet();
            } else {
                // If no "nodes" was found, try to look into { edges { node } } selection set
                List<Field> edgeFields = environment.getSelectionSet().get().get("edges");
                if (edgeFields != null) {
                    List<Selection> edgeSelection = edgeFields.get(0).getSelectionSet().getSelections();
                    for (Selection selection : edgeSelection) {
                        if (selection instanceof Field && ((Field) selection).getName().equals("node")) {
                            selectionSet = ((Field)selection).getSelectionSet();
                        }
                    }
                }
            }
        }

        return new FieldEvaluationEnvironment(type, selectionSet, getVariables(environment));
    }

    private static Map<String, Object> getVariables(DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getContext();
        Map<String, Object> variables = null;
        if (context.getRequest().isPresent()) {
            variables = (Map<String, Object>) context.getRequest().get().getAttribute(GRAPHQL_VARIABLES);
        }
        return variables;
    }

    /**
     * @return The GraphQLObject type which is returned by the field
     */
    public GraphQLObjectType getObjectType(Object object) {
        if (type instanceof GraphQLObjectType) {
            return (GraphQLObjectType) type;
        } else if (type instanceof GraphQLInterfaceType) {
            return ((GraphQLInterfaceType) type).getTypeResolver().getType(new TypeResolutionEnvironment(object, null, null, null, null, null));
        } else {
            return null;
        }
    }

    /**
     * @return Selection set on which we will look for the sub-field
     */
    public SelectionSet getSelectionSet() {
        return selectionSet;
    }

    /**
     * @return Global variable passed to the request
     */
    public Map<String, Object> getVariables() {
        return variables;
    }
}
