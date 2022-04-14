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
