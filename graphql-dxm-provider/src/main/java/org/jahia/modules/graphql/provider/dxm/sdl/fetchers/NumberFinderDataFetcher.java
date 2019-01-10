package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.apache.jackrabbit.util.Text;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.*;

public class NumberFinderDataFetcher extends FinderDataFetcher {

    private static final String GT = "gt";
    private static final String GTE = "gte";
    private static final String LT = "lt";
    private static final String LTE = "lte";
    private static final String EQ = "eq";
    private static final String NOTEQ = "!eq";
    private static final String PREVIEW = "preview";
    private static final String LANGUAGE = "language";


    public NumberFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> list = new ArrayList<>();
        list.add(GraphQLArgument.newArgument()
                .name(GT)
                .type(GraphQLLong)
                .description("Property greater than passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(GTE)
                .type(GraphQLLong)
                .description("Property greater than or equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(LT)
                .type(GraphQLLong)
                .description("Property less than passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(LTE)
                .type(GraphQLLong)
                .description("Property less than or equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(EQ)
                .type(GraphQLLong)
                .description("Property equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(NOTEQ)
                .type(GraphQLLong)
                .description("Property not equal to passed parameter")
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

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        try {
            String statement = "SELECT * FROM [%s] as n where n.[%s]%s%s";
            Map<String, Object> arguments = environment.getArguments();
            boolean invert = (Boolean)arguments.get("invert");

            String comparisonParameterName = firstValidParameter(arguments);
            if (comparisonParameterName == null)
                throw new DataFetchingException(String.format("Entry point %s must have of on comparison parameter, look at the documentation for available parameter names.", environment.getFieldDefinition().getName()));

            long argument = (long) arguments.get(comparisonParameterName);

            switch(comparisonParameterName) {
                case LT : statement = String.format(statement, type, finder.getProperty(), "<", argument);
                    break;
                case LTE : statement = String.format(statement, type, finder.getProperty(), "<=", argument);
                    break;
                case GT : statement = String.format(statement, type, finder.getProperty(), ">", argument);
                    break;
                case GTE : statement = String.format(statement, type, finder.getProperty(), ">=", argument);
                    break;
                case EQ : statement = String.format(statement, type, finder.getProperty(), "=", argument);
                    break;
                case NOTEQ : statement = String.format(statement, type, finder.getProperty(), "<>", argument);
                    break;
            }

            boolean preview = (Boolean) arguments.get(PREVIEW);
            Locale locale = LanguageCodeConverters.languageCodeToLocale((String) arguments.get(LANGUAGE));
            JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession(preview ? Constants.EDIT_WORKSPACE : Constants.LIVE_WORKSPACE, locale);
            JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

            return stream.collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private String firstValidParameter(Map<String, Object> arguments) {
        List<GraphQLArgument> args = getArguments();
        for (GraphQLArgument arg : args) {
            String argName = arg.getName();
            if (argName.equals(PREVIEW) || argName.equals(LANGUAGE)) continue;
            return argName;
        }
        return null;
    }
}
