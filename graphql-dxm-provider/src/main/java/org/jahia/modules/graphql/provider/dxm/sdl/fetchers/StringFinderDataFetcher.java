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

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

public class StringFinderDataFetcher extends FinderDataFetcher {

    private static final String CONTAINS = "contains";
    private static final String EQUALS = "equals";
    private static final String INVERT = "invert";


    public StringFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> list = getDefaultArguments();
        list.add(GraphQLArgument.newArgument()
                .name(CONTAINS)
                .type(GraphQLString)
                .description("Property contains passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(EQUALS)
                .type(GraphQLString)
                .description("Property is equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(INVERT)
                .type(GraphQLBoolean)
                .description("Inverts 'contains' or 'equals' argument to get either 'not contains' or 'not equals'. Default value is 'false'")
                .defaultValue(false)
                .build());
        return list;
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        try {
            String statement = String.format("SELECT * FROM [%s] as n where n.[%s]=''", type, finder.getProperty());
            Map<String, Object> arguments = environment.getArguments();
            boolean invert = (Boolean) arguments.get("invert");

            if (!arguments.containsKey(EQUALS) && !arguments.containsKey("contains"))
                throw new DataFetchingException(String.format("Entry point %s must have either 'contains' or 'equals' parameter", environment.getFieldDefinition().getName()));

            if (arguments.containsKey(CONTAINS)) {
                String argument = Text.escapeIllegalXpathSearchChars((String) arguments.get(CONTAINS));
                String addOn = invert ? "not" : "";
                statement = String.format("SELECT * FROM [%s] as n where %s contains(n.[%s], '%s')", type, addOn, finder.getProperty(), argument);
            } else if (arguments.containsKey(EQUALS)) {
                String argument = Text.escapeIllegalXpathSearchChars((String) arguments.get(EQUALS));
                String addOn = invert ? "<>" : "=";
                statement = String.format("SELECT * FROM [%s] as n where n.[%s]%s'%s'", type, finder.getProperty(), addOn, argument);
            }

            JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
            JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

            return stream.collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
