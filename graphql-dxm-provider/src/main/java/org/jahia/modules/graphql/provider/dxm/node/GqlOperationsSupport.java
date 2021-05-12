package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

@GraphQLDescription("Possible operations on a node")
public class GqlOperationsSupport {
    private GqlJcrNode node;

    public GqlOperationsSupport(GqlJcrNode node) {
        this.node = node;
    }

    public GqlJcrNode getNode() {
        return node;
    }

    @GraphQLField
    @GraphQLDescription("Can node be marked for deletion")
    public boolean getMarkForDeletion() {
        try {
            return node.getNode().canMarkForDeletion();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Can node be locked")
    public boolean getLock() {
        try {
            JCRNodeWrapper jcrNodeWrapper = node.getNode();
            return jcrNodeWrapper.getSession().getProviderSession(jcrNodeWrapper.getProvider()).getRepository().getDescriptorValue(Repository.OPTION_LOCKING_SUPPORTED).getBoolean();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
