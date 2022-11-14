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
package org.jahia.modules.graphql.provider.dxm.sdl;

import graphql.schema.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SDLUtil {

    private SDLUtil() {
    }

    /**
     * Creates an input type with name 'typeName'Input, populates it with fields matching 'args' and returns a new argument
     * 'typeName' of new input object type.
     *
     * @param typeName
     * @param args
     * @return
     */
    public static GraphQLArgument wrapArgumentsInType(String typeName, List<GraphQLArgument> args) {
        GraphQLInputObjectType.Builder newObject = GraphQLInputObjectType.newInputObject();
        GraphQLInputObjectType.Builder defaultObject = GraphQLInputObjectType.newInputObject();

        args.forEach(arg -> {
            newObject
                    .name(String.format("%s%s", typeName, SDLConstants.CONNECTION_ARGUMENTS_INPUT_SUFFIX))
                    .field(GraphQLInputObjectField.newInputObjectField()
                            .name(arg.getName())
                            .description(arg.getDescription())
                            .type(arg.getType())
                            .defaultValue(arg.getArgumentDefaultValue().getValue())
                            .build()
                    );

            if (arg.getArgumentDefaultValue().getValue() != null) {
                defaultObject
                        .name(String.format("%s%s", typeName, SDLConstants.CONNECTION_ARGUMENTS_INPUT_SUFFIX))
                        .field(GraphQLInputObjectField.newInputObjectField()
                                .name(arg.getName())
                                .description(arg.getDescription())
                                .type(arg.getType())
                                .defaultValue(arg.getArgumentDefaultValue().getValue())
                                .build()
                        );
            }
        });

        return GraphQLArgument.newArgument()
                .name(typeName)
                .description("Available arguments")
                .type(newObject.build())
                .defaultValue(defaultObject.build())
                .build();
    }

    /**
     * Returns argument value from environment, in case connection arguments are present (before, after etc.) it tries to
     * look for requested argument in a dedicated input object.
     *
     * @param argName
     * @param environment
     * @return
     */
    public static Object getArgument(String argName, DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        if (environment.getFieldDefinition().getName().endsWith(SDLConstants.CONNECTION_QUERY_SUFFIX)) {

            String name = environment.getFieldDefinition().getName().replace(SDLConstants.CONNECTION_QUERY_SUFFIX, SDLConstants.CONNECTION_ARGUMENTS_SUFFIX);
            Object argObject = args.get(name);

            //In this case we are handling default arguments
            if (argObject instanceof GraphQLInputObjectType) {
                GraphQLInputObjectField field = ((GraphQLInputObjectType) argObject).getField(argName);
                return field != null ? field.getInputFieldDefaultValue() : null;
            }

            return ((Map<String, Object>) args.get(name)).get(argName);
        } else {
            return args.get(argName);
        }
    }

    /**
     * Returns arguments, in case connection arguments are present (before, after etc.) it tries to return arguments from dedicated
     * input object.
     *
     * @param environment
     * @return
     */
    public static Map<String, Object> getArguments(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        if (environment.getFieldDefinition().getName().endsWith(SDLConstants.CONNECTION_QUERY_SUFFIX)) {
            String name = environment.getFieldDefinition().getName().replace(SDLConstants.CONNECTION_QUERY_SUFFIX, SDLConstants.CONNECTION_ARGUMENTS_SUFFIX);
            Object argObject = args.get(name);

            //In this case we are handling default arguments
            if (argObject instanceof GraphQLInputObjectType) {
                return ((GraphQLInputObjectType) argObject)
                        .getFields()
                        .stream()
                        .collect(Collectors.toMap(GraphQLInputObjectField::getName, f -> f.getInputFieldDefaultValue().getValue()));
            }

            return (Map<String, Object>) args.get(name);
        } else {
            return args;
        }
    }

    /**
     * Returns arguments size, in case connection arguments are present (before, after etc.) it tries to return size of dedicated
     * input object argument.
     *
     * @param environment
     * @return
     */
    public static int getArgumentsSize(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        if (environment.getFieldDefinition().getName().endsWith(SDLConstants.CONNECTION_QUERY_SUFFIX)) {
            String name = environment.getFieldDefinition().getName().replace(SDLConstants.CONNECTION_QUERY_SUFFIX, SDLConstants.CONNECTION_ARGUMENTS_SUFFIX);
            Object argObject = args.get(name);

            //In this case we are handling default arguments
            if (argObject instanceof GraphQLInputObjectType) {
                return ((GraphQLInputObjectType) argObject)
                        .getFields().size();
            }

            return ((Map<String, Object>) args.get(name)).size();
        } else {
            return args.size();
        }
    }

    /**
     * Check the given file name is a valid field definition in wrapped type
     *
     * @param fieldName
     * @param environment
     * @return true is field definition is in wrapped type
     */
    public static Boolean isFieldInWrappedTypeFields(String fieldName, DataFetchingEnvironment environment) {
        GraphQLList list;
        if (environment.getFieldDefinition().getType() instanceof GraphQLObjectType) {
            list = (GraphQLList) ((GraphQLObjectType) environment.getFieldDefinition().getType()).getFieldDefinition("nodes").getType();
        } else {
            list = (GraphQLList) environment.getFieldDefinition().getType();
        }
        GraphQLObjectType type = (GraphQLObjectType) list.getWrappedType();
        GraphQLFieldDefinition fieldDefinition = getFieldDefinition(fieldName, type);
        return fieldDefinition != null;
    }

    private static GraphQLFieldDefinition getFieldDefinition(String fieldName, GraphQLObjectType type) {
        GraphQLFieldDefinition fieldDefinition = null;
        String[] nameParts = fieldName.split("\\.");
        for (String part : nameParts) {
            GraphQLFieldDefinition fieldDef = type.getFieldDefinition(part);
            if (fieldDef == null)
                return null;
            Object f = fieldDef.getType();
            if (f instanceof GraphQLObjectType) {
                type = (GraphQLObjectType) f;
            } else {
                fieldDefinition = type.getFieldDefinition(part);
            }
        }
        return fieldDefinition;
    }
}
