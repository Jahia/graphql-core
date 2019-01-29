package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AllFinderDataFetcher extends FinderDataFetcher {

    public AllFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        try {
            String statement = "select * from [\"" + type + "\"]";
            JCRNodeIteratorWrapper it = getCurrentUserSession(environment)
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(statement, Query.JCR_SQL2)
                    .execute()
                    .getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));
            return resolveCollection(stream, environment);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
