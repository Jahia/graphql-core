package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLBoolean;

/**
 * Created at 10 Jan$
 *
 * @author chooliyip
 **/
public class BooleanFinderDataFetcher extends FinderListDataFetcher {

    private static final String VALUE = "value";

    BooleanFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.singletonList(GraphQLArgument
                .newArgument()
                .name(VALUE)
                .description("select content if boolean value true or false")
                .type(GraphQLBoolean)
                .defaultValue(true)
                .build());
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        if (hasValidArguments(environment)) {
            try {
                String statement = buildSQL2Statement(environment);
                JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
                JCRNodeIteratorWrapper it = currentUserSession
                        .getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
                Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                        .filter(node -> PermissionHelper.hasPermission(node, environment))
                        .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));
                return resolveCollection(stream, environment);
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        } else {
            throw new DataFetchingException("By boolean data fetcher needs 'value' argument");
        }
    }

    /**
     * Construct SQL2 statement with arguments
     *
     * @param environment
     * @return
     */
    private String buildSQL2Statement(DataFetchingEnvironment environment) {
        Boolean value = (Boolean) SDLUtil.getArgument(VALUE, environment);

        return "SELECT * FROM [" + type + "] WHERE [" + finder.getProperty() + "] = " + value;

    }

    /**
     * Argument of value is needed
     *
     * @param environment
     * @return
     */
    private boolean hasValidArguments(DataFetchingEnvironment environment) {
        return SDLUtil.getArgument(VALUE, environment) != null;
    }
}
