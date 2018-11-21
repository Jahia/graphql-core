/**
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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

/**
 * GraphQL representation of a vanity URL.
 */
@GraphQLName("VanityUrl")
public class GqlJcrVanityUrlInput {

    private Boolean active;
    private Boolean defaultMapping;
    private String language;
    private String url;
    /**
     *  Initializes an instance of this class.
     *
     * @param active Desired value of the active flag or null to keep existing value
     * @param defaultMapping Desired value of the default flag or null to keep existing value
     * @param language Desired vanity URL language or null to keep existing value
     * @param url Desired URL value or null to keep existing value
     */
    public GqlJcrVanityUrlInput(@GraphQLName("active") @GraphQLDescription("Desired value of the active flag or null to keep existing value") Boolean active,
                                @GraphQLName("defaultMapping") @GraphQLNonNull @GraphQLDescription("Desired value of the default flag or null to keep existing value") Boolean defaultMapping,
                                @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Desired vanity URL language or null to keep existing value") String language,
                                @GraphQLName("url") @GraphQLNonNull @GraphQLDescription("Desired URL value or null to keep existing value") String url) {
        this.active = active;
        this.defaultMapping = defaultMapping;
        this.language = language;
        this.url = url;
    }

    /**
     * Returns the vanity URL.
     *
     * @return the vanity URL
     */
    @GraphQLField
    @GraphQLName("url")
    @GraphQLNonNull
    @GraphQLDescription("The vanity URL")
    public String getUrl() {
        return url;
    }

    /**
     * Returns the language of the content object to which the vanity URL maps to.
     *
     * @return language of the mapping
     */
    @GraphQLField
    @GraphQLName("language")
    @GraphQLNonNull
    @GraphQLDescription("The language of the content object to which the vanity URL maps to")
    public String getLanguage() {
        return language;
    }

    /**
     * Returns true if the URL mapping is activated or false if it is not activated.
     *
     * @return true if the URL mapping is activated or false if it is not activated
     */
    @GraphQLField
    @GraphQLName("active")
    @GraphQLDescription("true if the URL mapping is activated or false if it is not activated")
    public Boolean isActive() {
        return active;
    }

    /**
     * Returns true whether this URL mapping is the default one for the language.
     *
     * @return <code>true</code> whether this URL mapping is the default one for the language, otherwise <code>false</code>
     */
    @GraphQLField
    @GraphQLName("defaultMapping")
    @GraphQLNonNull
    @GraphQLDescription("true whether this URL mapping is the default one for the language")
    public Boolean isDefaultMapping() {
        return defaultMapping;
    }
}
