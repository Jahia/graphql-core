package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.SDLSchemaService;
import org.jahia.osgi.BundleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

/**
 * Abstraction responsible for providing all necessary arguments and supporting methods to create a list of nodes finder
 * i. e. finder which returns a list of nodes.
 */
public abstract class FinderListDataFetcher extends FinderBaseDataFetcher {

    private static final String SORT_BY = "sortBy";
    private static final String FIELD_NAME = "fieldName";
    private static final String SORT_TYPE = "sortType";
    private static final String IGNORE_CASE = "ignoreCase";

    public FinderListDataFetcher(String type, Finder finder) {
        super(type, finder);
    }

    public FinderListDataFetcher(String type) {
        this(type, null);
    }

    @Override
    protected List<GraphQLArgument> getDefaultArguments() {
        SDLSchemaService sdlSchemaService = BundleUtils.getOsgiService(SDLSchemaService.class, null);
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

        if (sdlSchemaService == null) {
            //Technically this case should never happen but the method to retrieve the service can, by specification, return
            //null so this case needs to be handled.
            return list;
        }

        list.add(GraphQLArgument
                .newArgument()
                .name(SORT_BY)
                .description("Sort filter object")
                .type(sdlSchemaService.getSDLSpecialInputType(SDLSchemaService.SpecialInputTypes.FIELD_SORTER_INPUT.getName()))
                .build());
        return list;
    }

    protected Stream<GqlJcrNode> resolveCollection(Stream<GqlJcrNode> stream, DataFetchingEnvironment environment) {
        FieldSorterInput sorterInput = getFieldSorterInput(environment);
        if (sorterInput != null) {
            if (environment.getFieldType().getName() != null && environment.getFieldType().getName().endsWith(SDLConstants.CONNECTION_QUERY_SUFFIX)) {
                return stream.sorted(SorterHelper.getFieldComparator(sorterInput, FieldEvaluator.forConnection(environment)));
            }
            return stream.sorted(SorterHelper.getFieldComparator(sorterInput, FieldEvaluator.forList(environment)));
        } else {
            return stream;
        }
    }

    private FieldSorterInput getFieldSorterInput(DataFetchingEnvironment environment) {
        Map sortByFilter = (Map) SDLUtil.getArgument(SORT_BY, environment);
        return sortByFilter != null ? new FieldSorterInput((String) sortByFilter.get(FIELD_NAME), (SorterHelper.SortType) sortByFilter.get(SORT_TYPE), (Boolean) sortByFilter.get(IGNORE_CASE)) : null;
    }

    public abstract Object get(DataFetchingEnvironment environment);

    public abstract Stream<GqlJcrNode> getStream(DataFetchingEnvironment environment);
}

