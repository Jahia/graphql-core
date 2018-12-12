package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;

import static graphql.Scalars.GraphQLString;

public class ByPathFinderDataFetcher extends FinderDataFetcher {
    public ByPathFinderDataFetcher(String type, Finder finder) {
        super(type);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.singletonList(GraphQLArgument.newArgument().name("path").type(GraphQLString).build());
    }

    @Override
    public GqlJcrNode get(DataFetchingEnvironment environment) {
        try {
            return new GqlJcrNodeImpl(JCRSessionFactory.getInstance().getCurrentUserSession().getNode(environment.getArgument("path")));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
