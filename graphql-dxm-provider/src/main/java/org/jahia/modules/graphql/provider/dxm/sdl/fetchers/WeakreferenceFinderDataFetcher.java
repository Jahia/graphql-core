package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.apache.jackrabbit.util.Text;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.modules.graphql.provider.dxm.sdl.validation.ArgumentValidator;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.*;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

public class WeakreferenceFinderDataFetcher extends FinderListDataFetcher {

    private static final String PROPERTY = "property";
    private static final String CONTAINS = "contains";
    private static final String EQUALS = "equals";
    private static final String INVERT = "invert";

    private static Map<String, GraphQLInputType> enumTypes = new HashMap<>();


    public WeakreferenceFinderDataFetcher(WeakreferenceFinder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> list = getDefaultArguments();
        list.add(GraphQLArgument.newArgument()
                .name(PROPERTY)
                .type(makeArguments())
                .description("Property whose value is checked")
                .build());
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
        if (!ArgumentValidator.validate(ArgumentValidator.ArgumentNames.SORT_BY, environment)) {
            return Collections.emptyList();
        }

        return getStream(environment).collect(Collectors.toList());
    }

    @Override
    public Stream<GqlJcrNode> getStream(DataFetchingEnvironment environment) {
        if (!ArgumentValidator.validate(ArgumentValidator.ArgumentNames.SORT_BY, environment)) {
            return Stream.empty();
        }

        try {
            Map<String, Object> arguments = SDLUtil.getArguments(environment);

            if (!arguments.containsKey(PROPERTY) && (!arguments.containsKey(EQUALS) && !arguments.containsKey(CONTAINS)))
                throw new DataFetchingException(String.format("Entry point %s must have 'property' and either 'contains' or 'equals' parameters", environment.getFieldDefinition().getName()));

            String statement = "";
            String weakreferenceProp = Text.escapeIllegalXpathSearchChars((String) arguments.get(PROPERTY));
            boolean invert = (Boolean) arguments.get(INVERT);

            if (arguments.containsKey(CONTAINS)) {
                String argument = Text.escapeIllegalXpathSearchChars((String) arguments.get(CONTAINS));
                String addOn = invert ? "not" : "";
                statement = String.format("SELECT a.* FROM [%s] as a inner join [%s] as b on a.[%s] = b.['jcr:uuid'] where %s contains(b.['%s'], '%s')", type, ((WeakreferenceFinder) finder).getReferencedType(), finder.getProperty(), addOn, weakreferenceProp, argument);
            } else if (arguments.containsKey(EQUALS)) {
                String argument = Text.escapeIllegalXpathSearchChars((String) arguments.get(EQUALS));
                String addOn = invert ? "<>" : "=";
                statement = String.format("SELECT a.* FROM [%s] as a inner join [%s] as b on a.[%s] = b.['jcr:uuid'] where b.['%s']%s'%s'", type, ((WeakreferenceFinder) finder).getReferencedType(), finder.getProperty(), weakreferenceProp, addOn, argument);
            }

            JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
            JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));
            return resolveCollection(stream, environment);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private GraphQLInputType makeArguments() {
        String enumName = String.format("%sEnum", ((WeakreferenceFinder) finder).getReferencedTypeSDLName());

        if (enumTypes.containsKey(enumName)) {
            return enumTypes.get(enumName);
        }

        GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
                .name(enumName)
                .description("Available properties");

        for (Map.Entry<String, String> entry : ((WeakreferenceFinder) finder).getReferenceTypeProps().entrySet()) {
            if (entry.getValue().equals("identifier")) {
                builder.value(entry.getKey(), "jcr:uuid", String.format("%s property of type %s", entry.getKey(), ((WeakreferenceFinder) finder).getReferencedTypeSDLName()));
            } else if (entry.getValue().equals("path")) {
                builder.value(entry.getKey(), "j:fullpath", String.format("%s property of type %s", entry.getKey(), ((WeakreferenceFinder) finder).getReferencedTypeSDLName()));
            } else {
                builder.value(entry.getKey(), entry.getValue(), String.format("%s property of type %s", entry.getKey(), ((WeakreferenceFinder) finder).getReferencedTypeSDLName()));
            }
        }

        enumTypes.put(enumName, builder.build());
        return enumTypes.get(enumName);
    }
}
