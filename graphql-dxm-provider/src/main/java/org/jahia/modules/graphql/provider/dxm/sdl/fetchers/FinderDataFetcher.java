package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.SDLSchemaService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

public abstract class FinderDataFetcher implements DataFetcher {

    protected String type;
    protected Finder finder;

    static final String PREVIEW = "preview";
    static final String LANGUAGE = "language";

    private static final String SORT_BY = "sortBy";
    private static final String FIELD_NAME = "fieldName";
    private static final String SORT_TYPE = "sortType";
    private static final String IGNORE_CASE = "ignoreCase";

    public FinderDataFetcher(String type, Finder finder) {
        this.type = type.split(",")[0];
        this.finder = finder;
    }

    public FinderDataFetcher(String type) {
        this(type, null);
    }

    static Locale getLocale(DataFetchingEnvironment environment) {
        String language = environment.getArgument(LANGUAGE);
        if (language == null) return SettingsBean.getInstance().getDefaultLocale();
        return LanguageCodeConverters.languageCodeToLocale(language);
    }

    protected static JCRSessionWrapper getCurrentUserSession(DataFetchingEnvironment environment) throws RepositoryException {
        return getCurrentUserSession(environment, getLocale(environment));
    }

    protected static JCRSessionWrapper getCurrentUserSession(DataFetchingEnvironment environment, Locale locale) throws RepositoryException {
        Boolean preview = environment.getArgument(PREVIEW);
        if (preview == null) {
            preview = Boolean.FALSE;
        }
        return JCRSessionFactory.getInstance().getCurrentUserSession(preview ? Constants.EDIT_WORKSPACE : Constants.LIVE_WORKSPACE, locale);
    }

    public List<GraphQLArgument> getArguments() {
        return getDefaultArguments();
    }

    protected static List<GraphQLArgument> getDefaultArguments() {
        SDLSchemaService sdlSchemaService = BundleUtils.getOsgiService(SDLSchemaService.class, null);
        List<GraphQLArgument> list = new ArrayList<>();
        list.add(GraphQLArgument
                .newArgument()
                .name(SORT_BY)
                .description("sort filter object")
                .type(sdlSchemaService.getSdlSpecialInputType("FieldSorterInput"))
                .build());
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

    protected FieldSorterInput getFieldSorterInput(DataFetchingEnvironment environment){
        Map sortByFitler = environment.getArgument(SORT_BY);
        if(sortByFitler!=null){
            return new FieldSorterInput(
                    (String)sortByFitler.get(FIELD_NAME),
                    (SorterHelper.SortType) sortByFitler.get(SORT_TYPE),
                    (Boolean)sortByFitler.get(IGNORE_CASE)
            );
        } else {
            return null;
        }
    }

    public abstract Object get(DataFetchingEnvironment environment);
}

