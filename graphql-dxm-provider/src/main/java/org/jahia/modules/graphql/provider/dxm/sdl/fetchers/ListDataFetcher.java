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
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created at 23 Jan$
 *
 * @author chooliyip
 **/
public class ListDataFetcher extends FinderListDataFetcher {

    private static Logger logger = LoggerFactory.getLogger(ListDataFetcher.class);
    private Field field;

    public ListDataFetcher(Field field) {
        super("", null);
        this.field = field;
    }

    @Override
    public List get(DataFetchingEnvironment environment) {
        return getStream(environment).collect(Collectors.toList());
    }

    @Override
    public Stream<GqlJcrNode> getStream(DataFetchingEnvironment environment) {
        GqlJcrNode node = environment.getSource();
        JCRNodeWrapper jcrNode = node.getNode();
        if (environment.getFieldDefinition().getType() instanceof GraphQLObjectType) {
            //In this case we are dealing with connection at field level of a type i. e. text : TextConnection etc.
            GraphQLObjectType obj = (GraphQLObjectType) ((GraphQLList) ((GraphQLObjectType) environment.getFieldDefinition().getType()).getFieldDefinition("nodes").getType()).getWrappedType();
            GraphQLDirective mappingDirective = obj.getDirective(SDLConstants.MAPPING_DIRECTIVE);
            if (mappingDirective != null) {
                String nodeType = mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE).getValue().toString();
                return resolveChildren(jcrNode, nodeType);
            }
        }
        GraphQLType type = resolveWrappedObject(environment.getFieldDefinition().getType());
        if (type instanceof GraphQLObjectType) {
            GraphQLDirective mappingDirective = ((GraphQLObjectType) type).getDirective(SDLConstants.MAPPING_DIRECTIVE);
            GraphQLArgument arg = mappingDirective != null ? mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE) : null;
            return resolveFromArgument(jcrNode, arg);
        }
        try {
            return resolveProperty(jcrNode);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private GraphQLType resolveWrappedObject(GraphQLType type) {
        return type instanceof GraphQLList ? ((GraphQLList) type).getWrappedType() : type;
    }

    private Object getProperty(int propertyType, JCRValueWrapper value) {
        try {
            switch (propertyType) {
                case PropertyType.STRING:
                    return value.getString();
                case PropertyType.BOOLEAN:
                    return value.getBoolean();
                case PropertyType.DOUBLE:
                    return value.getDouble();
                case PropertyType.LONG:
                    return value.getLong();
                case PropertyType.WEAKREFERENCE:
                    logger.debug("Fetch weak reference {}", field.getProperty());
                    return new GqlJcrNodeImpl(value.getNode());
                default:
                    return null;
            }
        } catch (RepositoryException ex) {
            logger.error("Failed to retrieve node property {}", ex);
            return null;
        }
    }

    private Stream resolveProperty(JCRNodeWrapper jcrNode) throws RepositoryException {
        if (!jcrNode.hasProperty(field.getProperty())) {
            return Stream.empty();
        }

        int propertyType = jcrNode.getProperty(field.getProperty()).getType();
        return Arrays.stream(jcrNode.getProperty(field.getProperty()).getRealValues())
                .map(value -> getProperty(propertyType, value))
                .filter(Objects::nonNull);
    }

    private Stream<GqlJcrNode> resolveChildren(JCRNodeWrapper node, String nodeType) {
        return JCRContentUtils.getChildrenOfType(node, nodeType).stream()
                .map(GqlJcrNodeImpl::new);
    }

    private Stream resolveFromArgument(JCRNodeWrapper node, GraphQLArgument arg) {
        if (arg != null) {
            try {
                String nodeType = arg.getValue().toString();
                if (field == null) {
                    logger.debug("Fetch children of type {}", nodeType);
                    //Case when child name is not specified, get children directly from jcrNode
                    return resolveChildren(node, nodeType);
                }

                //Case when property is a weak reference
                if (node.hasProperty(field.getProperty())) {
                    return resolveProperty(node);
                }

                //Case when child mapping is specified, get children from mapped node
                logger.debug("Fetch children of type {} from child {}", nodeType, field.getProperty());
                return resolveChildren(node.getNode(field.getProperty()), nodeType);

            } catch (RepositoryException e) {
                //Do nothing, return empty list below
            }
        }
        return Stream.empty();
    }
}
