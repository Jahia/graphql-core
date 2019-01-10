package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;

import javax.jcr.RepositoryException;

public class StringPropertyDataFetcher implements DataFetcher<Object> {

    private Field field;

    public StringPropertyDataFetcher(Field field) {
        this.field = field;
    }

    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        try {
            GqlJcrNode node = dataFetchingEnvironment.getSource();


            JCRNodeWrapper jcrNodeWrapper = node.getNode();

            if (dataFetchingEnvironment.getArgument("language") != null) {
                jcrNodeWrapper = NodeHelper.getNodeInLanguage(jcrNodeWrapper, dataFetchingEnvironment.getArgument("language"));
            }

            if (!jcrNodeWrapper.hasProperty(field.getProperty())) {
                return null;
            }

            JCRPropertyWrapper property = jcrNodeWrapper.getProperty(field.getProperty());

            return property.getValue().getString();

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
