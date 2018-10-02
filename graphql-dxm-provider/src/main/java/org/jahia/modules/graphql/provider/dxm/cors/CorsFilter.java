/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
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
