package org.jahia.modules.graphql.provider.dxm.render;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.render.RenderContext;

@GraphQLTypeExtension(DXGraphQLJCRNode.class)
public class RenderNodeExtensions {

    @GraphQLField
    public static DXGraphQLJCRNodeImpl getDisplayableNode(DataFetchingEnvironment environment) {
        RenderContext context = new RenderContext(((GraphQLContext) environment.getContext()).getRequest().get(),
                ((GraphQLContext) environment.getContext()).getResponse().get(),
                JCRSessionFactory.getInstance().getCurrentUser());
        JCRNodeWrapper node = JCRContentUtils.findDisplayableNode(((DXGraphQLJCRNode) environment.getSource()).getNode(), context);
        if (node != null) {
            return new DXGraphQLJCRNodeImpl(node);
        } else {
            return null;
        }
    }

    @GraphQLField
    public static String getAjaxRenderUrl(DataFetchingEnvironment environment) {
        DXGraphQLJCRNode node = environment.getSource();
        return node.getNode().getUrl() + ".ajax";
    }

}
