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

import graphql.GraphQLContext;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.bin.Render;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrProperty;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.SelectorType;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.settings.SettingsBean;

import javax.jcr.PropertyType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@GraphQLTypeExtension(GqlJcrProperty.class)
@GraphQLDescription("Extensions for JCRProperty")
public class RenderPropertyExtensions {

    private GqlJcrProperty property;

    public RenderPropertyExtensions(GqlJcrProperty property) {
        this.property = property;
    }

    @GraphQLField
    @GraphQLDescription("Gets the rendered values of that property")
    public List<String> getRenderedValues(DataFetchingEnvironment environment) {
        try {
            if (!property.getProperty().getDefinition().isMultiple() ||
                    property.getProperty().getDefinition().getRequiredType() != PropertyType.STRING ||
                    ((ExtendedPropertyDefinition) property.getProperty().getDefinition()).getSelector() != SelectorType.RICHTEXT) {
                return null;
            }
            List<String> result = new ArrayList<>();
            for (String value: property.getValues()) {
                result.add(renderValue("html", property.getLanguage(), value, environment.getGraphQlContext()));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Gets the rendered value of that property")
    public String getRenderedValue(DataFetchingEnvironment environment) {
        try {
            if (property.getProperty().getDefinition().isMultiple() ||
                    property.getProperty().getDefinition().getRequiredType() != PropertyType.STRING ||
                    ((ExtendedPropertyDefinition) property.getProperty().getDefinition()).getSelector() != SelectorType.RICHTEXT) {
                return null;
            }
            return renderValue("html", property.getLanguage(), property.getValue(), environment.getGraphQlContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String renderValue(String templateType, String language, String value, GraphQLContext context) throws Exception {
        RenderService renderService = (RenderService) SpringContextSingleton.getBean("RenderService");

        if (language == null) {
            language = property.getNode().getNode().getResolveSite().getDefaultLanguage();
            if (language == null) {
                language = "en";
            }
        }

        HttpServletRequest request = ContextUtil.getHttpServletRequest(context);
        HttpServletResponse response = ContextUtil.getHttpServletResponse(context);
        if (request == null || response == null) {
            throw new RuntimeException("No HttpRequest or HttpResponse");
        }
        if (request instanceof HttpServletRequestWrapper) {
            request = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
        }


        JCRNodeWrapper node = NodeHelper.getNodeInLanguage(property.getNode().getNode(), language);

        Resource resource = new Resource(node, templateType, "richtext", "module");

        RenderContext renderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
        renderContext.setMainResource(resource);
        renderContext.setServletPath(Render.getRenderServletPath());
        renderContext.setWorkspace(node.getSession().getWorkspace().getName());

        JCRSiteNode site = node.getResolveSite();
        renderContext.setSite(site);

        request.setAttribute("value", value);
        response.setCharacterEncoding(SettingsBean.getInstance().getCharacterEncoding());

        Object savedSkipWrapper = request.getAttribute("skipWrapper");
        Object savedTemplateSet = request.getAttribute("templateSet");
        request.removeAttribute("skipWrapper");
        request.removeAttribute("templateSet");

        try {
            return RenderExtensionsHelper.clean(renderService.render(resource, renderContext));
        } finally {
            if (savedSkipWrapper != null) {
                request.setAttribute("skipWrapper", savedSkipWrapper);
            } else {
                request.removeAttribute("skipWrapper");
            }
            if (savedTemplateSet != null) {
                request.setAttribute("templateSet", savedTemplateSet);
            } else {
                request.removeAttribute("templateSet");
            }
        }
    }

}
