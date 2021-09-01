/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.graphql.context;

import graphql.kickstart.servlet.context.GraphQLServletContext;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Custom GraphQL context to inject file upload for testing
 */
public class CustomGraphQLServletContext implements GraphQLServletContext {

    GraphQLServletContext context;
    List<Part> files;

    public CustomGraphQLServletContext(GraphQLServletContext context, List<Part> files) {
        this.context = context;
        this.files = files;
    }

    @Override public List<Part> getFileParts() {
        return files;
    }

    @Override public Map<String, List<Part>> getParts() {
        // based from DefaultGraphQLServletContext implementation
        return files.stream().collect(Collectors.groupingBy(Part::getName));
    }

    @Override public HttpServletRequest getHttpServletRequest() {
        return context.getHttpServletRequest();
    }

    @Override public HttpServletResponse getHttpServletResponse() {
        return context.getHttpServletResponse();
    }

    @Override public Optional<Subject> getSubject() {
        return context.getSubject();
    }

    @Override public Optional<DataLoaderRegistry> getDataLoaderRegistry() {
        return context.getDataLoaderRegistry();
    }
}
