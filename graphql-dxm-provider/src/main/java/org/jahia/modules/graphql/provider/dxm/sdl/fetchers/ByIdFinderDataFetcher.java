package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;

import static graphql.Scalars.GraphQLString;

public class ByIdFinderDataFetcher extends FinderDataFetcher {

    public ByIdFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> defaultArguments = getDefaultArguments();
        defaultArguments.add(GraphQLArgument.newArgument().name("id").type(GraphQLString).build());
        return defaultArguments;
    }

    @Override
    public GqlJcrNode get(DataFetchingEnvironment environment) {
        try {
            return new GqlJcrNodeImpl(getCurrentUserSession(environment).getNodeByIdentifier(environment.getArgument("id")));
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
