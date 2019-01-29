/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 * <p>
 * http://www.jahia.com
 * <p>
 * Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 * <p>
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 * <p>
 * 1/ GPL
 * ==================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ===================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 * <p>
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created at 23 Jan$
 *
 * @author chooliyip
 **/
public class ListDataFetcher implements DataFetcher<List> {

    private static Logger logger = LoggerFactory.getLogger(ListDataFetcher.class);
    Field field;

    public ListDataFetcher(Field field) {
        this.field = field;
    }

    @Override
    public List get(DataFetchingEnvironment environment) throws Exception {
        GqlJcrNode node = environment.getSource();
        JCRNodeWrapper jcrNode = node.getNode();
        GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) environment.getFieldDefinition().getType()).getWrappedType();
        GraphQLDirective mappingDirective = type.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (mappingDirective != null) {
            GraphQLArgument nodeProperty = mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE);
            if (nodeProperty != null) {
                try {
                    String nodeType = nodeProperty.getValue().toString();
                    if (field == null) {
                        logger.debug("Fetch children of type {}", nodeType);
                        //Case when child name is not specified, get children directly from jcrNode
                        return JCRContentUtils.getChildrenOfType(jcrNode, nodeType).stream()
                                .map(GqlJcrNodeImpl::new)
                                .collect(Collectors.toList());
                    } else {
                        logger.debug("Fetch children of type {} from child {}", nodeType, field.getProperty());
                        //Case when child mapping is specified, get children from mapped node
                        JCRNodeWrapper subNode = jcrNode.getNode(field.getProperty());
                        return JCRContentUtils.getChildrenOfType(subNode, nodeType).stream()
                                .map(GqlJcrNodeImpl::new)
                                .collect(Collectors.toList());
                    }
                } catch (RepositoryException e) {
                    //Do nothing, return empty list below
                }
            }
        }
        return Collections.emptyList();
    }

}
