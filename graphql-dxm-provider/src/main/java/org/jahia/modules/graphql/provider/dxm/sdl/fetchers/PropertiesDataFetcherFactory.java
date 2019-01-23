package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRPropertyWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.stream.Collectors;

public class PropertiesDataFetcherFactory {

    private static Logger logger = LoggerFactory.getLogger(PropertiesDataFetcherFactory.class);

    public static DataFetcher getFetcher(GraphQLFieldDefinition graphQLFieldDefinition, Field field) {
        GraphQLDirective mapping = graphQLFieldDefinition.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (mapping != null) {
            GraphQLArgument property = mapping.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY);
            if (property != null) {
                String propertyValue = property.getValue().toString();
                if (SDLConstants.IDENTIFIER.equalsIgnoreCase(propertyValue)) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        return node.getUuid();
                    };
                } else if (SDLConstants.PATH.equalsIgnoreCase(propertyValue)) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        return node.getPath();
                    };
                } else if (propertyValue.startsWith(Constants.JCR_CONTENT) && propertyValue.contains(".")) {
                    return new FileContentFetcher(field, propertyValue.split("\\.")[1]);
                } else if (graphQLFieldDefinition.getType() instanceof GraphQLObjectType) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        JCRNodeWrapper jcrNode = node.getNode();
                        try {
                            logger.debug("Property reference to object type {}", field.getType());

                            JCRPropertyWrapper propertyNode = jcrNode.getProperty(field.getProperty());
                            return new GqlJcrNodeImpl(((JCRPropertyWrapperImpl) propertyNode).getNode());
                        } catch (RepositoryException e) {
                            return null;
                        }
                    };
                } else if (graphQLFieldDefinition.getType() instanceof GraphQLList) {
                    return environment -> {
                        GqlJcrNode node = environment.getSource();
                        JCRNodeWrapper jcrNode = node.getNode();
                        GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) environment.getFieldDefinition().getType()).getWrappedType();
                        GraphQLDirective mappingDirective = type.getDirective(SDLConstants.MAPPING_DIRECTIVE);
                        if (mappingDirective != null) {
                            GraphQLArgument nodeProperty = mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE);
                            if (nodeProperty != null) {
                                try {
                                    JCRNodeWrapper subNode = jcrNode.getNode(field.getProperty());
                                    String nodeType = nodeProperty.getValue().toString();
                                    return JCRContentUtils.getChildrenOfType(subNode, nodeType).stream()
                                            .map(GqlJcrNodeImpl::new)
                                            .collect(Collectors.toList());
                                }
                                catch (RepositoryException e) {
                                    //Do nothing, return empty list below
                                }
                            }
                        }
                        return Collections.EMPTY_LIST;
                    };
                }
            }
        }
        return new PropertiesDataFetcher(field);
    }
}
