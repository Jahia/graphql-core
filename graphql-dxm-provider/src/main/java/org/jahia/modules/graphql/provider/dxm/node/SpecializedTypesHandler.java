/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.TypeResolutionEnvironment;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.schema.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.sdl.types.GraphQLDate;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
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

    private GraphQLAnnotationsComponent graphQLAnnotations;
    private ProcessingElementsContainer container;

    private static Logger logger = LoggerFactory.getLogger(SpecializedTypesHandler.class);
    private static Pattern VALID_NAME = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private List<String> specializedTypes = new ArrayList<>();
    private Map<String, Class<? extends GqlJcrNode>> specializedTypesClass = new HashMap<>();

    private Map<String, GraphQLObjectType> knownTypes = new ConcurrentHashMap<>();

    private static SpecializedTypesHandler instance;

    public static SpecializedTypesHandler getInstance() {
        return instance;
    }

    public SpecializedTypesHandler(GraphQLAnnotationsComponent annotations, ProcessingElementsContainer container) {
        instance = this;

        this.graphQLAnnotations = annotations;
        this.container = container;
    }

    public void addType(String nodeType, Class<? extends GqlJcrNode> clazz) {
        if (clazz == null) {
            specializedTypes.add(nodeType);
        } else {
            specializedTypesClass.put(nodeType, clazz);
        }
        logger.debug("Registered specialized type {} handled by class {}", nodeType, clazz);
    }

    public Map<String, GraphQLObjectType> getKnownTypes() {
        return knownTypes;
    }

    public void initializeTypes() {
        knownTypes = new HashMap<>();
        GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNode.class, container);
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
            knownTypes.put(entry.getKey(), (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(entry.getValue(), container));
        }
    }

    private GraphQLObjectType createGraphQLType(ExtendedNodeType type, String typeName, GraphQLInterfaceType interfaceType) {
        final String escapedTypeName = escape(typeName);
        logger.debug("Creating {}", escapedTypeName);

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(escapedTypeName)
                .withInterface(interfaceType)
                .fields(((GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, container)).getFieldDefinitions());

        final PropertyDefinition[] properties = type.getPropertyDefinitions();
        if (properties.length > 0) {
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
                }
            }
        }

        final NodeDefinition[] children = type.getChildNodeDefinitions();
        if (children.length > 0) {
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
                type = new GraphQLDate();
                break;
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                type = GraphQLLong;
                break;
            case PropertyType.DOUBLE:
                type = GraphQLFloat;
                break;
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                type = graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNode.class, container);
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
            SpecializedTypesHandler instance = SpecializedTypesHandler.getInstance();
            if (instance.knownTypes.containsKey(type)) {
                return instance.knownTypes.get(type);
            } else {
                return (GraphQLObjectType) instance.graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNodeImpl.class, instance.container);
            }
        }
    }
}
