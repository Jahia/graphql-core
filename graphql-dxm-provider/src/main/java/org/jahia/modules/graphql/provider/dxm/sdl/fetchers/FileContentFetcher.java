package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;

public class FileContentFetcher implements DataFetcher {
    private Field field;
    private String fileProperty;

    public FileContentFetcher(Field field, String fileProperty) {
        this.field = field;
        this.fileProperty = fileProperty;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        try {
            GqlJcrNode node = environment.getSource();


            JCRNodeWrapper jcrNodeWrapper = node.getNode();

            if (environment.getArgument("language") != null) {
                jcrNodeWrapper = NodeHelper.getNodeInLanguage(jcrNodeWrapper, environment.getArgument("language"));
            }

            if (fileProperty.equalsIgnoreCase("contentType")) {
                return jcrNodeWrapper.getFileContent().getContentType();
            } else if (fileProperty.equalsIgnoreCase("contentLength")) {
                return jcrNodeWrapper.getFileContent().getContentLength();
            }
            return fileProperty + "is unknown on JCRContentFile";
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
