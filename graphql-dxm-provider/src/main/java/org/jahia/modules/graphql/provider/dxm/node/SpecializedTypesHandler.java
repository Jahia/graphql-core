/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
