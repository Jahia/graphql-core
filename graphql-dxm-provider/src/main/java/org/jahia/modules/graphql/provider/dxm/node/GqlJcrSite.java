package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
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
