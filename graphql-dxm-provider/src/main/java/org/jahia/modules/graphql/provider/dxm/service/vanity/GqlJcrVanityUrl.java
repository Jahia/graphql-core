package org.jahia.modules.graphql.provider.dxm.service.vanity;
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

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedType;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

import static org.jahia.api.Constants.JCR_LANGUAGE;
import static org.jahia.services.seo.jcr.VanityUrlManager.*;

/**
 * GraphQL representation of a vanity URL
 */
@GraphQLName("VanityUrl")
@SpecializedType("jnt:vanityUrl")
public class GqlJcrVanityUrl extends GqlJcrNodeImpl implements GqlJcrNode {

    private JCRNodeWrapper vanityNode;

    public GqlJcrVanityUrl(JCRNodeWrapper node) {
        super(node);
        this.vanityNode = node;
    }

    @GraphQLField
    public String getUrl() {
        return vanityNode.getPropertyAsString(PROPERTY_URL);
    }

    @GraphQLField
    public String getLanguage() {
        return vanityNode.getPropertyAsString(JCR_LANGUAGE);
    }

    @GraphQLField
    public boolean isActive() throws RepositoryException {
        return vanityNode.hasProperty(PROPERTY_ACTIVE) && vanityNode.getProperty(PROPERTY_ACTIVE).getBoolean();
    }

    @GraphQLField
    public boolean  isDefault() throws RepositoryException {
        return vanityNode.hasProperty(PROPERTY_DEFAULT) && vanityNode.getProperty(PROPERTY_DEFAULT).getBoolean();
    }
}
