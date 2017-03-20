package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;

class DisplayNameDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        try {
            JCRNodeWrapper jcrNode = node.getNode();
            String language = dataFetchingEnvironment.getArgument("language");
            if (language != null) {
                jcrNode = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(jcrNode.getIdentifier());
            }
            return jcrNode.getDisplayableName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
