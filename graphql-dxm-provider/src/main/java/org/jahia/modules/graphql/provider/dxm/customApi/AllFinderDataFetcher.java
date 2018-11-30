package org.jahia.modules.graphql.provider.dxm.customApi;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AllFinderDataFetcher extends FinderDataFetcher {
    public AllFinderDataFetcher(String type) {
        super(type);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.emptyList();
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        try {
            String statement = "select * from [\"" + type + "\"]";

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
