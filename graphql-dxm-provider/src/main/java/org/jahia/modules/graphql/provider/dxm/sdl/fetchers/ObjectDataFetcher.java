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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRPropertyWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Created at 23 Jan$
 *
 * @author chooliyip
 **/
public class ObjectDataFetcher implements DataFetcher<Object> {

    private static Logger logger = LoggerFactory.getLogger(ObjectDataFetcher.class);

    private Field field;

    public ObjectDataFetcher(Field field) {
        this.field = field;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        GqlJcrNode node = environment.getSource();
        JCRNodeWrapper jcrNode = node.getNode();
        try {
            if (jcrNode.hasNode(field.getProperty())) {
                logger.debug("Fetch child {}", field.getType());
                //Treat property as child
                JCRNodeWrapper subNode = jcrNode.getNode(field.getProperty());
                return new GqlJcrNodeImpl(subNode);
            } else {
                logger.debug("Fetch reference {}", field.getType());
                //Treat property as weak reference
                JCRPropertyWrapper propertyNode = jcrNode.getProperty(field.getProperty());
                return new GqlJcrNodeImpl(((JCRPropertyWrapperImpl) propertyNode).getReferencedNode());
            }
        } catch (RepositoryException e) {
            return null;
        }
    }

}
