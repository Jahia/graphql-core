/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.render;


import graphql.annotations.annotationTypes.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.kickstart.servlet.context.GraphQLServletContext;
import org.jahia.bin.Render;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ConstraintsHelper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.services.uicomponents.bean.editmode.EditConfiguration;
import org.jahia.settings.SettingsBean;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@GraphQLTypeExtension(GqlJcrNode.class)
public class RenderNodeExtensions {

    private GqlJcrNode node;

    public RenderNodeExtensions(GqlJcrNode node) {
        this.node = node;
    }

    /**
     * Returns the first parent of the current node that can be displayed in full page.
     * If no matching node is found, null is returned.
     *
     * @return the first parent of the current node that can be displayed in full page. If no matching node is found, null is returned.
     */
    @GraphQLField
    @GraphQLDescription("Returns the first parent of the current node that can be displayed in full page. If no matching node is found, null is returned.")
    public GqlJcrNode getDisplayableNode(DataFetchingEnvironment environment) {
        GraphQLServletContext gqlContext = environment.getContext();

        HttpServletRequest httpServletRequest = ContextUtil.getHttpServletRequest(environment.getContext());
        HttpServletResponse httpServletResponse = ContextUtil.getHttpServletResponse(environment.getContext());

        if (httpServletRequest == null || httpServletResponse == null) {
            return null;
        }

        RenderContext context = new RenderContext(httpServletRequest,
                httpServletResponse,
                JCRSessionFactory.getInstance().getCurrentUser());
        JCRNodeWrapper node = JCRContentUtils.findDisplayableNode(((GqlJcrNode) environment.getSource()).getNode(), context);
        if (node != null) {
            try {
                return SpecializedTypesHandler.getNode(node);
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Check if the node as a renderable template associated with it (not a view a template).
     *
     * @return true if the node as a renderable template associated with it
     */
    @GraphQLField
    @GraphQLName("isDisplayableNode")
    @GraphQLDescription("Check if the node as a renderable template associated with it (not a view a template).")
    public boolean isDisplayableNode() {
        try {
            final RenderContext context = new RenderContext(null, null, node.getNode().getSession().getUser());
            context.setMainResource(new Resource(node.getNode(), "html", null, Resource.CONFIGURATION_PAGE));
            context.setServletPath("/cms/render/live");
            return JCRContentUtils.isADisplayableNode(node.getNode(), context);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Render URL in ajax mode")
    public String getAjaxRenderUrl() {
        return node.getNode().getUrl() + ".ajax";
    }

    @GraphQLField
    @GraphQLDescription("Gets the fully rendered content for this node")
    public RenderedNode getRenderedContent(@GraphQLName("view") @GraphQLDescription("Name of the view") String view,
                                           @GraphQLName("templateType") @GraphQLDescription("Template type") String templateType,
                                           @GraphQLName("contextConfiguration") @GraphQLDescription("Rendering context configuration") String contextConfiguration,
                                           @GraphQLName("language") @GraphQLDescription("Language") String language,
                                           @GraphQLName("mainResourcePath") @GraphQLDescription("Main resource path") String mainResourcePath,
                                           @GraphQLName("isEditMode") @GraphQLDescription("Is edit mode") Boolean isEditMode,
                                           @GraphQLName("requestAttributes") @GraphQLDescription("Additional request attributes") Collection<RenderRequestAttributeInput> requestAttributes, DataFetchingEnvironment environment) {
        try {
            RenderService renderService = (RenderService) SpringContextSingleton.getBean("RenderService");

            if (contextConfiguration == null) {
                contextConfiguration = "preview";
            }
            if (templateType == null) {
                templateType = "html";
            }

            if (language == null) {
                language = node.getNode().getResolveSite().getDefaultLanguage();
                if (language == null) {
                    language = "en";
                }
            }

            HttpServletRequest request = ContextUtil.getHttpServletRequest(environment.getContext());
            HttpServletResponse response = ContextUtil.getHttpServletResponse(environment.getContext());
            if (request == null || response == null) {
                throw new RuntimeException("No HttpRequest or HttpResponse");
            }

            if (request instanceof HttpServletRequestWrapper) {
                request = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
            }

            if (requestAttributes != null && requestAttributes.size() > 0) {
                for (RenderRequestAttributeInput requestAttribute : requestAttributes) {
                    request.setAttribute(requestAttribute.getName(), requestAttribute.getValue());
                }
            }

            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(this.node.getNode(), language);

            Resource r = new Resource(node, templateType, view, contextConfiguration);

            RenderContext renderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
            if (mainResourcePath != null) {
                JCRNodeWrapper mainNode = NodeHelper.getNodeInLanguage(node.getSession().getNode(mainResourcePath), language);
                renderContext.setMainResource(new Resource(mainNode, templateType, view, contextConfiguration));
            } else {
                renderContext.setMainResource(r);
            }
            if (isEditMode != null && isEditMode) {
                renderContext.setEditMode(true);
                renderContext.setEditModeConfig((EditConfiguration) SpringContextSingleton.getBean("editmode-jahia-anthracite"));
            }

            renderContext.setServletPath(Render.getRenderServletPath());
            renderContext.setWorkspace(node.getSession().getWorkspace().getName());

            JCRSiteNode site = node.getResolveSite();
            renderContext.setSite(site);

            response.setCharacterEncoding(SettingsBean.getInstance().getCharacterEncoding());
            String res = renderService.render(r, renderContext);

            return new RenderedNode(res, renderContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLDescription("Rendering result for a node")
    public static class RenderedNode {
        private String output;
        private RenderContext renderContext;

        public RenderedNode(String output, RenderContext renderContext) {
            this.output = output;
            this.renderContext = renderContext;
        }

        @GraphQLField
        @GraphQLDescription("Rendering output")
        public String getOutput() {
            return output;
        }

        @GraphQLField
        @GraphQLDescription("Contraints on this node")
        public String getConstraints() {
            String constraints = null;
            try {
                constraints = ConstraintsHelper.getConstraints(renderContext.getMainResource().getNode());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            return constraints;
        }

        @GraphQLField
        @GraphQLDescription("List of static assets")
        public List<StaticAsset> getStaticAssets(@GraphQLName("type") @GraphQLDescription("Assets type") @GraphQLNonNull String type) {
            Map<String, Map<String, Map<String, String>>> staticAssets = (Map) renderContext.getRequest().getAttribute("staticAssets");
            if (staticAssets != null) {
                Map<String, Map<String, String>> entries = staticAssets.get(type);
                if (entries != null) {
                    List<StaticAsset> result = new ArrayList<>();
                    for (Map.Entry<String, Map<String, String>> filetypeEntries : entries.entrySet()) {
                        String filePath = filetypeEntries.getKey();
                        Map<String, String> fileOptions = filetypeEntries.getValue();
                        result.add(new StaticAsset(filePath, fileOptions));
                    }
                    return result;
                }
            }
            return null;
        }
    }

    @GraphQLDescription("Representation of a static assert")
    public static class StaticAsset {
        private String key;
        private Map<String, String> options;

        public StaticAsset(String key, Map<String, String> options) {
            this.key = key;
            this.options = options;
        }

        @GraphQLField
        @GraphQLDescription("Asset key")
        public String getKey() {
            return key;
        }

        @GraphQLField
        @GraphQLDescription("Asset option")
        public String getOption(@GraphQLName("name") @GraphQLDescription("Asset option name") @GraphQLNonNull String name) {
            return options.get(name);
        }

    }

}
