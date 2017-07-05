package org.jahia.modules.graphql.provider.dxm.node;


import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNode;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNodeImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;

@GraphQLName("JCRSite")
public class DXGraphQLJCRSite extends DXGraphQLJCRNodeImpl implements DXGraphQLJCRNode {

    private JCRSiteNode siteNode;

    public DXGraphQLJCRSite(JCRNodeWrapper node) {
        super(node);
        this.siteNode = (JCRSiteNode) node;
    }

    @GraphQLField
    public String getSiteKey() {
        return siteNode.getSiteKey();
    }
}
