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
package org.jahia.modules.graphql.provider.dxm.cors;


import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component(service = {Filter.class}, property = {"pattern=/graphql"}, immediate = true)
public class CorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    private static final List<String> ALLOWED_HEADERS = Arrays.asList("authorization", "content-type");

    private DXGraphQLConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Reference
    public void setConfig(DXGraphQLConfig config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        if (servletResponse instanceof HttpServletResponse && servletRequest instanceof HttpServletRequest) {
            final HttpServletResponse response = (HttpServletResponse) servletResponse;
            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            final String origin = request.getHeader("origin");

            if (StringUtils.isNotBlank(origin)) {
                if (checkOrigin(origin)) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                        String[] requestHeaders = StringUtils.split(request.getHeader("Access-Control-Request-Headers"), ", ");
                        List<String> filteredHeader = Arrays.stream(requestHeaders).map(String::toLowerCase).filter(ALLOWED_HEADERS::contains).collect(Collectors.toList());
                        response.setHeader("Access-Control-Allow-Credentials", "true");
                        response.addHeader("Access-Control-Allow-Headers", StringUtils.join(filteredHeader,","));
                    }
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    public boolean checkOrigin(String origin) {
        return config.getCorsOrigins().contains(origin);
    }

}
