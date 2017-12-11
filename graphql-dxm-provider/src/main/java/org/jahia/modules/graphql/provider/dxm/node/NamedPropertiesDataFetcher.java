/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class NamedPropertiesDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        String name = dataFetchingEnvironment.getFields().get(0).getName();
        name = SpecializedTypesHandler.unescape(StringUtils.substringAfter(name, SpecializedTypesHandler.PROPERTY_PREFIX));

        try {
            GqlJcrNode node = (GqlJcrNode) dataFetchingEnvironment.getSource();
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            if (!jcrNodeWrapper.hasProperty(name)) {
                return null;
            }
            JCRPropertyWrapper property = jcrNodeWrapper.getProperty(name);

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
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return SpecializedTypesHandler.getNode(value.getNode());
            default:
                return value.getString();
        }
    }
}
