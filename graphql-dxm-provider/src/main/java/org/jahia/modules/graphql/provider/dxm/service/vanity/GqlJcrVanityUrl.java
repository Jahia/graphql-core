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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.GraphQLDescription;
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
 * GraphQL representation of a vanity URL.
 */
@GraphQLName("VanityUrl")
@SpecializedType("jnt:vanityUrl")
public class GqlJcrVanityUrl extends GqlJcrNodeImpl implements GqlJcrNode {

    /**
     * Initializes an instance of this class.
     *
     * @param vanityNode the JCR node that corresponds to the vanity URL
     */
    public GqlJcrVanityUrl(JCRNodeWrapper vanityNode) {
        super(vanityNode);
    }

    /**
     * Returns the vanity URL.
     *
     * @return the vanity URL
     */
    @GraphQLField
    @GraphQLName("url")
    @GraphQLDescription("The vanity URL")
    public String getUrl() {
        return getNode().getPropertyAsString(PROPERTY_URL);
    }

    /**
     * Returns the language of the content object to which the vanity URL maps to.
     *
     * @return language of the mapping
     */
    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("The language of the content object to which the vanity URL maps to")
    public String getLanguage() {
        return getNode().getPropertyAsString(JCR_LANGUAGE);
    }

    /**
     * Returns the node targeted by this vanity URL.
     *
     * @return The node targeted by this vanity URL
     */
    @GraphQLField
    @GraphQLName("targetNode")
    @GraphQLDescription("The node targeted by this vanity URL")
    public GqlJcrNode getTargetNode() {
        return getParent().getParent();
    }

    /**
     * Returns true if the URL mapping is activated or false if it is not activated.
     *
     * @return true if the URL mapping is activated or false if it is not activated
     */
    @GraphQLField
    @GraphQLName("active")
    @GraphQLDescription("true if the URL mapping is activated or false if it is not activated")
    public boolean isActive() {
        try {
            return getNode().hasProperty(PROPERTY_ACTIVE) && getNode().getProperty(PROPERTY_ACTIVE).getBoolean();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true whether this URL mapping is the default one for the language.
     *
     * @return <code>true</code> whether this URL mapping is the default one for the language, otherwise <code>false</code>
     */
    @GraphQLField
    @GraphQLName("default")
    @GraphQLDescription("true whether this URL mapping is the default one for the language")
    public boolean isDefault() {
        try {
            return getNode().hasProperty(PROPERTY_DEFAULT) && getNode().getProperty(PROPERTY_DEFAULT).getBoolean();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
