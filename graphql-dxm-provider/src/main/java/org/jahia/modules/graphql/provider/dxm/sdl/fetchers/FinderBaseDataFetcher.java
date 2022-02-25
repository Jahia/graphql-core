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
import graphql.schema.GraphQLArgument;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

/**
 * Abstraction responsible for providing all necessary arguments and supporting methods to create a single node finder
 * i. e. finder which returns a single node. Which means that the set of arguments will contain only universally mandatory
 * preview and language.
 */
public abstract class FinderBaseDataFetcher implements DataFetcher {

    protected static final String PREVIEW = "preview";
    protected static final String LANGUAGE = "language";
    protected String type;
    protected Finder finder;

    public FinderBaseDataFetcher(String type, Finder finder) {
        this.type = type.split(",")[0];
        this.finder = finder;
    }

    public FinderBaseDataFetcher(String type) {
        this(type, null);
    }

    protected static Locale getLocale(DataFetchingEnvironment environment) {
        String language = (String) SDLUtil.getArgument(LANGUAGE, environment);
        if (language == null) return SettingsBean.getInstance().getDefaultLocale();
        return LanguageCodeConverters.languageCodeToLocale(language);
    }

    protected static JCRSessionWrapper getCurrentUserSession(DataFetchingEnvironment environment) throws RepositoryException {
        return getCurrentUserSession(environment, getLocale(environment));
    }

    protected static JCRSessionWrapper getCurrentUserSession(DataFetchingEnvironment environment, Locale locale) throws RepositoryException {
        Boolean preview = (Boolean) SDLUtil.getArgument(PREVIEW, environment);
        if (preview == null) {
            preview = Boolean.FALSE;
        }
        return JCRSessionFactory.getInstance().getCurrentUserSession(preview ? Constants.EDIT_WORKSPACE : Constants.LIVE_WORKSPACE, locale);
    }

    protected List<GraphQLArgument> getDefaultArguments() {
        List<GraphQLArgument> list = new ArrayList<>();
        list.add(GraphQLArgument.newArgument()
                .name(PREVIEW)
                .type(GraphQLBoolean)
                .description("Return content from live or default workspace")
                .defaultValue(false)
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(LANGUAGE)
                .type(GraphQLString)
                .description("Content language, defaults to English")
                .defaultValue("en")
                .build());
        return list;
    }

    public List<GraphQLArgument> getArguments() {
        return getDefaultArguments();
    }

    public abstract Object get(DataFetchingEnvironment environment);
}

