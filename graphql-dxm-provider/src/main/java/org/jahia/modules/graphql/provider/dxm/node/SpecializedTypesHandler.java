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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.TypeResolutionEnvironment;
import graphql.annotations.processor.GraphQLAnnotationsComponent;
import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpecializedTypesHandler {

    private GraphQLAnnotationsComponent graphQLAnnotations;
    private ProcessingElementsContainer container;

    private static Logger logger = LoggerFactory.getLogger(SpecializedTypesHandler.class);

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
        specializedTypesClass.put(nodeType, clazz);
        logger.debug("Registered specialized type {} handled by class {}", nodeType, clazz);
    }

    public Map<String, GraphQLObjectType> getKnownTypes() {
        return knownTypes;
    }

    public void initializeTypes() {
        knownTypes = new HashMap<>();
        GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(GqlJcrNode.class, container);
        for (Map.Entry<String, Class<? extends GqlJcrNode>> entry : specializedTypesClass.entrySet()) {
            knownTypes.put(entry.getKey(), (GraphQLObjectType) graphQLAnnotations.getOutputTypeProcessor().getOutputTypeOrRef(entry.getValue(), container));
        }
    }

    public static GqlJcrNode getNode(JCRNodeWrapper node) throws RepositoryException {
        return getNode(node, node.getPrimaryNodeTypeName());
    }

    public static GqlJcrNode getNode(JCRNodeWrapper node, String type) throws RepositoryException {
        node = NodeHelper.getNodeInLanguage(node, null);
        if (!NodeHelper.checkNodeValidity(node)) {
            throw new ItemNotFoundException(node.getIdentifier());
        }
        if (getInstance().specializedTypesClass.containsKey(type)) {
            try {
                return getInstance().specializedTypesClass.get(type).getConstructor(JCRNodeWrapper.class).newInstance(node);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new GqlJcrNodeImpl(node, type);
        }
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
