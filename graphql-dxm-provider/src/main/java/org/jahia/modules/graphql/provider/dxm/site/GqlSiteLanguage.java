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
package org.jahia.modules.graphql.provider.dxm.site;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.utils.LanguageCodeConverters;

import java.util.Locale;

@GraphQLName("JCRSiteLanguage")
@GraphQLDescription("Site language representation")
public class GqlSiteLanguage {

    private String language;

    private boolean activeInEdit;
    private boolean activeInLive;
    private boolean mandatory;

    public GqlSiteLanguage(String language, boolean activeInEdit, boolean activeInLive, boolean mandatory) {
        this.language = language;
        this.activeInEdit = activeInEdit;
        this.activeInLive = activeInLive;
        this.mandatory = mandatory;
    }

    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("Language code")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLName("displayName")
    @GraphQLDescription("Display name")
    public String getDisplayName(@GraphQLName("language") @GraphQLDescription("Language") String displayLanguage) {
        Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
        return displayLanguage != null ? locale.getDisplayName(Locale.forLanguageTag(displayLanguage)) : locale.getDisplayName(locale);
    }

    @GraphQLField
    @GraphQLDescription("Is this language active in edit")
    public boolean isActiveInEdit() {
        return activeInEdit;
    }

    @GraphQLField
    @GraphQLDescription("Is this language active in live")
    public boolean isActiveInLive() {
        return activeInLive;
    }

    @GraphQLField
    @GraphQLDescription("Is this language mandatory")
    public boolean isMandatory() {
        return mandatory;
    }
}
