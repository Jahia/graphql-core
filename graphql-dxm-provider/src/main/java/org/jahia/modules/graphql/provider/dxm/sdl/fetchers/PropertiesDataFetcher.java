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

            String fieldProperty = field.getProperty();
            if (fieldProperty.contains(".")) {
                String[] propertyName = fieldProperty.split("\\.");
                if (!jcrNodeWrapper.hasNode(propertyName[0])) {
                    return null;
                }
                jcrNodeWrapper = jcrNodeWrapper.getNode(propertyName[0]);
                fieldProperty = propertyName[1];
            }

            if (!jcrNodeWrapper.hasProperty(fieldProperty)) {
                return null;
            }


            JCRPropertyWrapper property = jcrNodeWrapper.getProperty(fieldProperty);

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
            default:
                return value.getString();
        }
    }
}
