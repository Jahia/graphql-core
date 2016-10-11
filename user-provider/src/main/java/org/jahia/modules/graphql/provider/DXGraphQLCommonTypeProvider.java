package org.jahia.modules.graphql.provider;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * A common class to provide types common to mutations and queries.
 */
public class DXGraphQLCommonTypeProvider {

    private static Logger logger = LoggerFactory.getLogger(DXGraphQLCommonTypeProvider.class);

    public static GraphQLObjectType getPropertyType() {
        return newObject()
                .name("property")
                .field(newFieldDefinition()
                        .name("key")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("value")
                        .type(GraphQLString)
                        .build())
                .build();
    }

    public static GraphQLObjectType getDXNodeType() {
        return newObject()
                .name("node")
                .field(newFieldDefinition()
                        .name("identifier")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("path")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("parentPath")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("parentIdentifier")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("primaryNodeType")
                        .type(GraphQLString)
                        .build())
                .field(newFieldDefinition()
                        .name("mixinTypes")
                        .type(new GraphQLList(GraphQLString))
                        .build())
                .field(newFieldDefinition()
                        .name("properties")
                        .type(new GraphQLList(getPropertyType()))
                        .build())
                .build();
    }

    public static GraphQLOutputType getGraphQLType(int jcrPropertyType, boolean multiValued) {
        if (multiValued) {
            switch (jcrPropertyType) {
                case PropertyType.BOOLEAN:
                    return new GraphQLList(GraphQLBoolean);
                case PropertyType.DATE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                    return new GraphQLList(GraphQLLong);
                case PropertyType.DOUBLE:
                    return new GraphQLList(GraphQLFloat);
                case PropertyType.BINARY:
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                case PropertyType.URI:
                case PropertyType.WEAKREFERENCE:
                    return new GraphQLList(GraphQLString);
            }
        } else {
            switch (jcrPropertyType) {
                case PropertyType.BOOLEAN:
                    return GraphQLBoolean;
                case PropertyType.DATE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                    return GraphQLLong;
                case PropertyType.DOUBLE:
                    return GraphQLFloat;
                case PropertyType.BINARY:
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                case PropertyType.URI:
                case PropertyType.WEAKREFERENCE:
                    return GraphQLString;
            }
        }
        logger.warn("Couldn't find equivalent GraphQL type for property type=" + jcrPropertyType + " will use string type instead !");
        return GraphQLString;
    }

}
