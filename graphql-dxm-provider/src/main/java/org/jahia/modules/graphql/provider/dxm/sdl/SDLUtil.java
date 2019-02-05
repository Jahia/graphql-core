package org.jahia.modules.graphql.provider.dxm.sdl;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

import java.util.List;
import java.util.Map;

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
        args.forEach(arg -> newObject
                .name(String.format("%s%s", typeName, SDLConstants.CONNECTION_ARGUMENTS_INPUT_SUFFIX))
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name(arg.getName())
                        .description(arg.getDescription())
                        .type(arg.getType())
                        .defaultValue(arg.getDefaultValue())
                        .build()
                ));

        return GraphQLArgument.newArgument()
                .name(typeName)
                .description("Available arguments")
                .type(newObject.build())
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
            return ((Map<String, Object>) args.get(name)).size();
        } else {
            return args.size();
        }
    }
}
