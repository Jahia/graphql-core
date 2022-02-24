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
@GraphQLDescription("GraphQL representation of a vanity URL")
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
