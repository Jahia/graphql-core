package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Collections;
import java.util.List;

import static graphql.Scalars.GraphQLString;

public class StringFinderDataFetcher extends FinderDataFetcher {
    public StringFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.singletonList(GraphQLArgument.newArgument().name("eq").type(GraphQLString).build());
    }

    @Override
    public GqlJcrNode get(DataFetchingEnvironment environment) {
        try {
            String statement = "select * from [\"" + type + "\"] where [\"" + finder.getProperty() + "\"]=\"" + environment.getArgument("eq") + "\"";
            NodeIterator it = JCRSessionFactory.getInstance().getCurrentUserSession().getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            if (it.hasNext()) {
                return new GqlJcrNodeImpl((JCRNodeWrapper) it.nextNode());
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
