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
