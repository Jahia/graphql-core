package org.jahia.modules.graphql.provider.dxm.node;

import graphql.TypeResolutionEnvironment;
import graphql.annotations.GraphQLAnnotations;
import graphql.annotations.GraphQLAnnotationsProcessor;
import graphql.schema.*;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

public class SpecializedTypesHandler {

    public static final String PROPERTY_PREFIX = "property_";
    public static final String UNNAMED_PROPERTY_PREFIX = "property_";
    public static final String CHILD_PREFIX = "child_";
    public static final String UNNAMED_CHILD_PREFIX = "child_";

    private static GraphQLAnnotationsProcessor graphQLAnnotations;

    private static Logger logger = LoggerFactory.getLogger(SpecializedTypesHandler.class);
    private static Pattern VALID_NAME = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private List<String> specializedTypes = new ArrayList<>();
    private Map<String, Class<? extends GqlJcrNode>> specializedTypesClass = new HashMap<>();

    private Map<String, GraphQLObjectType> knownTypes = new ConcurrentHashMap<>();

    private static SpecializedTypesHandler instance;

    public static SpecializedTypesHandler getInstance() {
        return instance;
    }

    public SpecializedTypesHandler(GraphQLAnnotationsProcessor annotations) {
        instance = this;
        graphQLAnnotations = annotations;
        specializedTypes.add("jnt:page");
        specializedTypesClass.put("jnt:virtualsite", GqlJcrSite.class);
    }

    public Map<String, GraphQLObjectType> getKnownTypes() {
        return knownTypes;
    }

    public void initializeTypes() {
        knownTypes = new HashMap<>();
        GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) graphQLAnnotations.getOutputType(GqlJcrNode.class);
        for (String typeName : specializedTypes) {
            try {
                final ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(typeName);
                final String escapedTypeName = escape(type.getName());
                if (!knownTypes.containsKey(typeName)) {
                    knownTypes.put(typeName, createGraphQLType(type, escapedTypeName, interfaceType));
                } else {
                    logger.debug("Already generated {}", escapedTypeName);
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            }
        }
        for (Map.Entry<String, Class<? extends GqlJcrNode>> entry : specializedTypesClass.entrySet()) {
            knownTypes.put(entry.getKey(), graphQLAnnotations.getObject(entry.getValue()));
        }
    }

    private GraphQLObjectType createGraphQLType(ExtendedNodeType type, String typeName, GraphQLInterfaceType interfaceType) {
        final String escapedTypeName = escape(typeName);
        logger.debug("Creating {}", escapedTypeName);

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(escapedTypeName)
                .withInterface(interfaceType)
                .fields(graphQLAnnotations.getObject(GqlJcrNodeImpl.class).getFieldDefinitions());

        final PropertyDefinition[] properties = type.getPropertyDefinitions();
        if (properties.length > 0) {
            final Set<String> multiplePropertyTypes = new HashSet<>(properties.length);
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
                    // Unnamed properties
//                    final String propertyTypeName = PropertyType.nameFromValue(propertyType);
//                    if (!multiplePropertyTypes.contains(propertyTypeName)) {
//                        builder.field(
//                                newFieldDefinition()
//                                        .name(UNNAMED_PROPERTY_PREFIX + propertyTypeName)
//                                        .type(getGraphQLType(propertyType, false))
//                                        .dataFetcher(new UnnamedPropertiesDataFetcher())
//                                        .build()
//                        );
//                        multiplePropertyTypes.add(propertyTypeName);
//                    }
                }
            }
        }

        final NodeDefinition[] children = type.getChildNodeDefinitions();
        if (children.length > 0) {
            final Set<String> multipleChildTypes = new HashSet<>(children.length);
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
                    // Unnamed children
//                    final String childTypeName = getChildTypeName(child);
//                    if (!multipleChildTypes.contains(childTypeName)) {
//                        final String escapedChildTypeName = escape(childTypeName);
//                        builder.field(
//                                newFieldDefinition()
//                                        .name(UNNAMED_CHILD_PREFIX + escapedChildTypeName)
//                                        .type(new GraphQLList(new GraphQLTypeReference(specializedTypes.contains(childTypeName) ? escapedChildTypeName : "GenericJCRNode")))
//                                        .dataFetcher(new UnnamedChildNodesDataFetcher())
//                                        .build()
//                        );
//                        multipleChildTypes.add(childTypeName);
//                    }
                }
            }
        }

        builder.description(type.getDescription(Locale.ENGLISH));

        return builder.build();
    }


    private String getChildTypeName(NodeDefinition child) {
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

    private GraphQLOutputType getGraphQLType(int jcrPropertyType, boolean multiValued) {
        GraphQLOutputType type;
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
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                type = graphQLAnnotations.getOutputTypeOrRef(GqlJcrNode.class);
                break;
            case PropertyType.BINARY:
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
            case PropertyType.URI:
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

    public static GqlJcrNode getNode(JCRNodeWrapper node) throws RepositoryException {
        return getNode(node, node.getPrimaryNodeTypeName());
    }

    public static GqlJcrNode getNode(JCRNodeWrapper node, String type) throws RepositoryException {
        if (getInstance().specializedTypesClass.containsKey(type)) {
            try {
                return getInstance().specializedTypesClass.get(type).getConstructor(new Class[] {JCRNodeWrapper.class}).newInstance(node);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new GqlJcrNodeImpl(node, type);
        }
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

    public static class NodeTypeResolver implements TypeResolver {
        @Override
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            String type = ((GqlJcrNode) env.getObject()).getType();
            if (getInstance().knownTypes.containsKey(type)) {
                return getInstance().knownTypes.get(type);
            } else {
                return graphQLAnnotations.getObject(GqlJcrNodeImpl.class);
            }
        }
    }
}
