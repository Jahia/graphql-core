/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.render;


import graphql.ErrorType;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.jahia.bin.Render;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ConstraintsHelper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.settings.SettingsBean;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@GraphQLTypeExtension(GqlJcrNode.class)
public class RenderNodeExtensions {

    private GqlJcrNode node;

    public RenderNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    @GraphQLField
    public GqlJcrNode getDisplayableNode(DataFetchingEnvironment environment) {
        RenderContext context = new RenderContext(((GraphQLContext) environment.getContext()).getRequest().get(),
                ((GraphQLContext) environment.getContext()).getResponse().get(),
                JCRSessionFactory.getInstance().getCurrentUser());
        JCRNodeWrapper node = JCRContentUtils.findDisplayableNode(((GqlJcrNode) environment.getSource()).getNode(), context);
        if (node != null) {
            try {
                return SpecializedTypesHandler.getNode(node);
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        } else {
            return null;
        }
    }

    @GraphQLField
    public String getAjaxRenderUrl() {
        return node.getNode().getUrl() + ".ajax";
    }

    @GraphQLField
    public RenderedNode getRenderedContent(@GraphQLName("view") String view, @GraphQLName("templateType") String templateType, @GraphQLName("contextConfiguration") String contextConfiguration,
                                        @GraphQLName("language") String language, DataFetchingEnvironment environment) {
        try {
            RenderService renderService = (RenderService) SpringContextSingleton.getBean("RenderService");

            if (contextConfiguration == null) {
                contextConfiguration = "module";
            }

            if (language == null) {
                language = node.getNode().getResolveSite().getDefaultLanguage();
                if (language == null) {
                    language = "en";
                }
            }

            HttpServletRequest request = ((GraphQLContext) environment.getContext()).getRequest().get();
            HttpServletResponse response = ((GraphQLContext) environment.getContext()).getResponse().get();

            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node.getNode(), language);

            Resource r = new Resource(node, templateType, view, contextConfiguration);

            RenderContext renderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
            renderContext.setMainResource(r);

            renderContext.setServletPath(Render.getRenderServletPath());

            JCRSiteNode site = node.getResolveSite();
            renderContext.setSite(site);

            response.setCharacterEncoding(SettingsBean.getInstance().getCharacterEncoding());
            String res = renderService.render(r, renderContext);

            return new RenderedNode(res, renderContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class RenderedNode {
        private String output;
        private RenderContext renderContext;

        public RenderedNode(String output, RenderContext renderContext) {
            this.output = output;
            this.renderContext = renderContext;
        }

        @GraphQLField
        public String getOutput() {
            return output;
        }

        @GraphQLField
        public String getConstraints() {
            String constraints = null;
            try {
                constraints = ConstraintsHelper.getConstraints(renderContext.getMainResource().getNode());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            return constraints;
        }

    }

}
