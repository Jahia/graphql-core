package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;

public class GqlNews  {

    private  GqlJcrNodeImpl node;

    public GqlNews(JCRNodeWrapper node) {
        this.node = new GqlJcrNodeImpl(node);
    }

    @GraphQLField
    public String getUuid() {
        return node.getUuid();
    }

    @GraphQLField
    public String getDescription(@GraphQLName("language") @GraphQLNonNull String language) {
        return node.getProperty("desc",language).getValue();
    }

    @GraphQLField
    public String getTitle(@GraphQLName("language") @GraphQLNonNull String language) {
        return node.getProperty("jcr:title",language).getValue();
    }

    @GraphQLField
    public GqlJcrNode getFile() {
        return node.getProperty("date", null).getRefNode();
    }

    @GraphQLField
    public String getDate() {
        return node.getProperty("date", null).getValue();
    }

}
