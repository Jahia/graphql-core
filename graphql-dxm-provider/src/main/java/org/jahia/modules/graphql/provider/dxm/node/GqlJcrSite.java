package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;

@GraphQLName("JCRSite")
public class GqlJcrSite extends GqlJcrNodeImpl implements GqlJcrNode {

    private JCRSiteNode siteNode;

    public GqlJcrSite(JCRNodeWrapper node) {
        super(node);
        this.siteNode = (JCRSiteNode) node;
    }

    @GraphQLField
    public String getSiteKey() {
        return siteNode.getSiteKey();
    }
}
