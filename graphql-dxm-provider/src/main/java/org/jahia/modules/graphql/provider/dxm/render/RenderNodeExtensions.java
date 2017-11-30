package org.jahia.modules.graphql.provider.dxm.render;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.render.RenderContext;

@GraphQLTypeExtension(GqlJcrNode.class)
public class RenderNodeExtensions {

    @GraphQLField
    public static GqlJcrNodeImpl getDisplayableNode(DataFetchingEnvironment environment) {
        RenderContext context = new RenderContext(((GraphQLContext) environment.getContext()).getRequest().get(),
                ((GraphQLContext) environment.getContext()).getResponse().get(),
                JCRSessionFactory.getInstance().getCurrentUser());
        JCRNodeWrapper node = JCRContentUtils.findDisplayableNode(((GqlJcrNode) environment.getSource()).getNode(), context);
        if (node != null) {
            return new GqlJcrNodeImpl(node);
        } else {
            return null;
        }
    }

    @GraphQLField
    public static String getAjaxRenderUrl(DataFetchingEnvironment environment) {
        GqlJcrNode node = environment.getSource();
        return node.getNode().getUrl() + ".ajax";
    }

}
