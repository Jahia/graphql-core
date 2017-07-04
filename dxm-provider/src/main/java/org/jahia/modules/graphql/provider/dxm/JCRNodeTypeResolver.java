package org.jahia.modules.graphql.provider.dxm;

import graphql.TypeResolutionEnvironment;
import graphql.annotations.GraphQLAnnotations;
import graphql.schema.*;
import org.jahia.api.Constants;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

public class JCRNodeTypeResolver implements TypeResolver {

    public static final String PROPERTY_PREFIX = "property_";
    public static final String UNNAMED_PROPERTY_PREFIX = "property_";
    public static final String CHILD_PREFIX = "child_";
    public static final String UNNAMED_CHILD_PREFIX = "child_";

    private static Logger logger = LoggerFactory.getLogger(JCRNodeTypeResolver.class);
    private static Pattern VALID_NAME = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private static List<String> specializedTypes = Arrays.asList("jnt:virtualsite");
    private static Map<String, GraphQLObjectType> knownTypes = new ConcurrentHashMap<>();

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        String type = ((DXGraphQLJCRNode) env.getObject()).getType();
        if (specializedTypes.contains(type)) {
            return getKnownTypes().get(escape(type));
        } else {
            return GraphQLAnnotations.getInstance().getObject(DXGraphQLGenericJCRNode.class);
        }
    }

    public static  Map<String, GraphQLObjectType> getKnownTypes() {
        if (knownTypes.isEmpty()) {
            GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) GraphQLAnnotations.getInstance().getInterface(DXGraphQLJCRNode.class);
            for (String typeName : specializedTypes) {
                try {
                    final ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(typeName);
                    final String escapedTypeName = escape(type.getName());
                    if (!knownTypes.containsKey(escapedTypeName)) {
                        knownTypes.put(escapedTypeName, createGraphQLType(type, escapedTypeName, interfaceType));
                    } else {
                        logger.debug("Already generated {}", escapedTypeName);
                    }
                } catch (NoSuchNodeTypeException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return knownTypes;
    }

    private static GraphQLObjectType createGraphQLType(ExtendedNodeType type, String typeName, GraphQLInterfaceType interfaceType) {
        final String escapedTypeName = escape(typeName);
        logger.debug("Creating {}", escapedTypeName);

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(escapedTypeName)
                .withInterface(interfaceType)
                .fields(GraphQLAnnotations.getInstance().getObject(DXGraphQLGenericJCRNode.class).getFieldDefinitions());

        final PropertyDefinition[] properties = type.getPropertyDefinitions();
        if (properties.length > 0) {
            final Set<String> multiplePropertyTypes = new HashSet<>(properties.length);
//            final GraphQLFieldDefinition.Builder propertiesField = newFieldDefinition().name("namedProperties").dataFetcher(identityDataFetcher);
//            final GraphQLObjectType.Builder propertiesType = newObject().name(escapedTypeName + "Properties");
            for (PropertyDefinition property : properties) {
                final String propName = property.getName();
                final int propertyType = property.getRequiredType();
                final boolean multiple = property.isMultiple();
                if (!"*".equals(propName)) {
                    final String escapedPropName = PROPERTY_PREFIX + escape(propName);
                    builder.field(newFieldDefinition()
                            .name(escapedPropName)
                            .dataFetcher(new NamedPropertiesDataFetcher())
                            .type(getGraphQLType(propertyType, multiple))
                            .build());
                } else {
                    final String propertyTypeName = PropertyType.nameFromValue(propertyType);
                    if (!multiplePropertyTypes.contains(propertyTypeName)) {
                        builder.field(
                                newFieldDefinition()
                                        .name(UNNAMED_PROPERTY_PREFIX + propertyTypeName)
                                        .type(getGraphQLType(propertyType, false))
                                        .dataFetcher(new UnnamedPropertiesDataFetcher())
//                                        .argument(newArgument()
//                                                .name("name")
//                                                .type(GraphQLString)
//                                                .build())
                                        .build()
                        );
                        multiplePropertyTypes.add(propertyTypeName);
                    }
                }
            }
//            propertiesField.type(propertiesType.build());
//            builder.field(propertiesField);
        }

        final NodeDefinition[] children = type.getChildNodeDefinitions();
        if (children.length > 0) {
            final Set<String> multipleChildTypes = new HashSet<>(children.length);
//            final GraphQLFieldDefinition.Builder childrenField = newFieldDefinition().name("namedChildren").dataFetcher(identityDataFetcher);
//            final GraphQLObjectType.Builder childrenType = newObject().name(escapedTypeName + "Children");
            for (NodeDefinition child : children) {
                final String childName = child.getName();

                if (!"*".equals(childName)) {
                    final String escapedChildName = CHILD_PREFIX + escape(childName);
                    final String childTypeName = getChildTypeName(child);
                    GraphQLOutputType gqlChildType = new GraphQLTypeReference(specializedTypes.contains(childTypeName) ? escape(childTypeName) : "GenericJCRNode");
                    builder.field(newFieldDefinition()
                            .name(escapedChildName)
                            .type(gqlChildType)
                            .dataFetcher(new NamedChildDataFetcher())
                            .build());
                } else {
                    final String childTypeName = getChildTypeName(child);
                    if (!multipleChildTypes.contains(childTypeName)) {
                        final String escapedChildTypeName = escape(childTypeName);
                        builder.field(
                                newFieldDefinition()
                                        .name(UNNAMED_CHILD_PREFIX + escapedChildTypeName)
                                        .type(new GraphQLList(new GraphQLTypeReference(specializedTypes.contains(childTypeName) ? escapedChildTypeName : "GenericJCRNode")))
                                        .dataFetcher(new UnnamedChildNodesDataFetcher())
//                                        .argument(newArgument()
//                                                .name("name")
//                                                .type(GraphQLString)
//                                                .build())
                                        .build()
                        );
                        multipleChildTypes.add(childTypeName);
                    }
                }
            }
//            childrenField.type(childrenType.build());
//            builder.field(childrenField);
        }

        builder.description(type.getDescription(Locale.ENGLISH));

        return builder.build();
    }


    private static String getChildTypeName(NodeDefinition child) {
        String childTypeName = child.getDefaultPrimaryTypeName();
        if (childTypeName == null) {
            final String[] primaryTypeNames = child.getRequiredPrimaryTypeNames();
            if (primaryTypeNames.length > 1) {
                // todo: do something here
                logger.warn("Multiple primary types (" + primaryTypeNames +
                        ") for child " + child.getName() + " of type "
                        + child.getDeclaringNodeType().getName());
                childTypeName = Constants.NT_BASE;
            } else {
                childTypeName = primaryTypeNames[0];
            }
        }
        return childTypeName;
    }

    private static GraphQLOutputType getGraphQLType(int jcrPropertyType, boolean multiValued) {
        GraphQLScalarType type;
        switch (jcrPropertyType) {
            case PropertyType.BOOLEAN:
                type = GraphQLBoolean;
                break;
            case PropertyType.DATE:
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                type = GraphQLLong;
                break;
            case PropertyType.DOUBLE:
                type = GraphQLFloat;
                break;
            case PropertyType.BINARY:
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
                type = GraphQLString;
                break;
            default:
                logger.warn("Couldn't find equivalent GraphQL type for "
                        + PropertyType.nameFromValue(jcrPropertyType)
                        + " property type will use string type instead!");
                type = GraphQLString;
        }

        return multiValued ? new GraphQLList(type) : type;
    }

    public static String escape(String name) {
        name = name.replace(":", "__").replace(".", "___");
        if (!VALID_NAME.matcher(name).matches()) {
            logger.error("Invalid name: " + name);
        }
        return name;
    }

    public static String unescape(String name) {
        return name.replace("___", ".").replace("__", ":");
    }
}
