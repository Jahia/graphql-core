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
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class PropertiesDataFetcher implements DataFetcher<Object> {

    private Field field;

    public PropertiesDataFetcher(Field field) {
        this.field = field;
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        try {
            GqlJcrNode node = dataFetchingEnvironment.getSource();


            JCRNodeWrapper jcrNodeWrapper = node.getNode();

            if (SDLUtil.getArgument("language", dataFetchingEnvironment) != null) {
                jcrNodeWrapper = NodeHelper.getNodeInLanguage(jcrNodeWrapper, (String) SDLUtil.getArgument("language", dataFetchingEnvironment));
            }

            JCRPropertyWrapper property = getProperty(jcrNodeWrapper);

            if (property == null) {
                return null;
            }

            if (!property.isMultiple()) {
                return getString(property.getValue());
            } else {
                List<Object> res = new ArrayList<>();
                for (JCRValueWrapper value : property.getValues()) {
                    res.add(getString(value));
                }
                return res;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private JCRPropertyWrapper getProperty(JCRNodeWrapper jcrNodeWrapper) throws RepositoryException {
        JCRPropertyWrapper property = null;
        String[] propertyNames = StringUtils.split(field.getProperty(), '.');
        for (String propertyName : propertyNames) {
            property = null;
            if (jcrNodeWrapper != null) {
                if (jcrNodeWrapper.hasNode(propertyName)) {
                    jcrNodeWrapper = jcrNodeWrapper.getNode(propertyName);
                } else if (jcrNodeWrapper.hasProperty(propertyName)) {
                    property = jcrNodeWrapper.getProperty(propertyName);
                    if (property.getType() == PropertyType.REFERENCE || property.getType() == PropertyType.WEAKREFERENCE) {
                        try {
                            jcrNodeWrapper = property.getValue().getNode();
                        } catch (RepositoryException e) {
                            jcrNodeWrapper = null;
                        }
                    }
                }
            }
        }
        return property;
    }

    private Object getString(JCRValueWrapper value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.BINARY:
                return value.getBinary().getSize();
            default:
                return value.getString();
        }
    }
}
