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
