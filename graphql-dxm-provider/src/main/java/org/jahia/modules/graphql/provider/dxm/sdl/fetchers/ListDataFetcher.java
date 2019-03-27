/*
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
