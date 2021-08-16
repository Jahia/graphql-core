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

    @Override public DataLoaderRegistry getDataLoaderRegistry() {
        return context.getDataLoaderRegistry();
    }
}
