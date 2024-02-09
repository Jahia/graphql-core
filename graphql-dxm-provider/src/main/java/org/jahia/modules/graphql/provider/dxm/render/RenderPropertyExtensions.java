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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.poifs.property.PropertyTable;
import org.jahia.bin.Render;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrProperty;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.settings.SettingsBean;

import javax.jcr.PropertyType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.regex.Pattern;

@GraphQLTypeExtension(GqlJcrProperty.class)
@GraphQLDescription("Extensions for JCRProperty")
public class RenderPropertyExtensions {

    private static final Pattern CLEANUP_REGEXP = Pattern.compile("<!-- jahia:temp [^>]*-->");

    private GqlJcrProperty property;

    public RenderPropertyExtensions(GqlJcrProperty property) {
        this.property = property;
    }

    @GraphQLField
    @GraphQLDescription("Gets the rendered content values of that node")
    public String[] getRichTextValues(@GraphQLName("templateType") @GraphQLDescription("Template type") String templateType,
                                        @GraphQLName("language") @GraphQLDescription("Language") String language,
                                        @GraphQLName("requestAttributes") @GraphQLDescription("Additional request attributes") Collection<RenderRequestAttributeInput> requestAttributes, DataFetchingEnvironment environment) {
        try {
            if (!property.getProperty().getDefinition().isMultiple() ||
                    property.getProperty().getDefinition().getRequiredType() != PropertyType.STRING ) {
                return null;
            }
            return new String[] {"NOT IMPLEMENTED"};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Gets the rendered content value of that node")
    public String getRichTextValue(@GraphQLName("templateType") @GraphQLDescription("Template type") String templateType,
                                       @GraphQLName("language") @GraphQLDescription("Language") String language,
                                       @GraphQLName("requestAttributes") @GraphQLDescription("Additional request attributes") Collection<RenderRequestAttributeInput> requestAttributes, DataFetchingEnvironment environment) {
        try {
            //TODO Ensure to test that property is able to be rendered (rich-text and single value)
            //TODO Maybe add a constraint on definition selector type:  - text (string, richtext) primary internationalized
            if (property.getProperty().getDefinition().isMultiple() ||
                    property.getProperty().getDefinition().getRequiredType() != PropertyType.STRING ) {
                return null;
            }

            RenderService renderService = (RenderService) SpringContextSingleton.getBean("RenderService");

            if (templateType == null) {
                templateType = "html";
            }

            if (language == null) {
                language = property.getNode().getNode().getResolveSite().getDefaultLanguage();
                if (language == null) {
                    language = "en";
                }
            }

            HttpServletRequest request = ContextUtil.getHttpServletRequest(environment.getGraphQlContext());
            HttpServletResponse response = ContextUtil.getHttpServletResponse(environment.getGraphQlContext());
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

            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(property.getNode().getNode(), language);

            Resource resource = new Resource(node, templateType, "richtext", "module");

            RenderContext renderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
            renderContext.setMainResource(resource);
            renderContext.setServletPath(Render.getRenderServletPath());
            renderContext.setWorkspace(node.getSession().getWorkspace().getName());

            JCRSiteNode site = node.getResolveSite();
            renderContext.setSite(site);

            request.setAttribute("propertyName", property.getName());
            response.setCharacterEncoding(SettingsBean.getInstance().getCharacterEncoding());
            String result = renderService.render(resource, renderContext);
            return (StringUtils.isNotEmpty(result))?CLEANUP_REGEXP.matcher(result).replaceAll(""):result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
