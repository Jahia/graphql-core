package org.jahia.modules.graphql.provider.dxm.model;

import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLNodeBuilder;
import org.jahia.services.content.JCRPropertyWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * TODO Comment me
 *
 * @author toto
 */
public class DXGraphQLProperty {
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLProperty.class);
    private JCRPropertyWrapper propertyWrapper;

    public DXGraphQLProperty(JCRPropertyWrapper propertyWrapper) {
        this.propertyWrapper = propertyWrapper;
    }

    public String getKey() {
        try {
            return propertyWrapper.getName();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String getValue() {
        try {
            return propertyWrapper.getValue().getString();
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
