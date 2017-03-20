package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLProperty;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

class PropertyDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        try {
            String name = dataFetchingEnvironment.getArgument("name");
            String language = dataFetchingEnvironment.getArgument("language");
            JCRNodeWrapper jcrNode = node.getNode();
            if (language != null) {
                jcrNode = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(jcrNode.getIdentifier());
            }
            if (jcrNode.hasProperty(name)) {
                return new DXGraphQLProperty(jcrNode.getProperty(name));
            }
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
