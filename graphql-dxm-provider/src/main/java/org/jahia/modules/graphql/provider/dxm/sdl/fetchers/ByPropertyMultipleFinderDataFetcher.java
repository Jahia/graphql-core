package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLString;

public class ByPropertyMultipleFinderDataFetcher extends FinderDataFetcher {
    public ByPropertyMultipleFinderDataFetcher(String type, Finder finder) {
        super(type, finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.singletonList(GraphQLArgument.newArgument().name("eq").type(GraphQLString).build());
    }

    @Override
    public List<GqlJcrNode>  get(DataFetchingEnvironment environment) {
        try {
            String statement = "select * from [\"" + type + "\"] where [\"" + finder.getProperty() + "\"]=\"" + environment.getArgument("eq") + "\"";
            JCRNodeIteratorWrapper it = JCRSessionFactory.getInstance().getCurrentUserSession().getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>)it, Spliterator.ORDERED), false)
                    .filter(node-> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

            return stream.collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
