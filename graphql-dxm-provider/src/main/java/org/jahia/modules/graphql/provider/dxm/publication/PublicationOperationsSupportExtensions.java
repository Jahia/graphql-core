package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlOperationsSupport;
import org.jahia.services.content.JCRPublicationService;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlOperationsSupport.class)
public class PublicationOperationsSupportExtensions {

    private GqlJcrNode gqlJcrNode;

    public PublicationOperationsSupportExtensions(GqlOperationsSupport gqlOperationsSupport) {
        this.gqlJcrNode = gqlOperationsSupport.getNode();
    }

    /**
     * Returns if the node supports publication
     *
     * @return  does the node supports publication
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("does the node supports publication")
    public boolean publication() {
        try {
            return JCRPublicationService.supportsPublication(gqlJcrNode.getNode().getSession(), gqlJcrNode.getNode());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
