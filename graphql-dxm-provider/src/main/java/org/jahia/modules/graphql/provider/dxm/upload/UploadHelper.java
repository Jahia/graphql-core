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
package org.jahia.modules.graphql.provider.dxm.upload;

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.settings.SettingsBean;

import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Get FileItem from multipart request.
 */
public class UploadHelper {

    /**
     * Check if the specified value matches a part in the request.
     *
     * @param name        Name of the part
     * @param environment The DataFetchingEnvironment
     * @return true if a FileItem is found
     * @throws FileSizeLimitExceededException if the file exceeds currently set limit
     */
    public static boolean isValidFileUpload(String name, DataFetchingEnvironment environment) throws FileSizeLimitExceededException {
        try {
            List<Part> parts = getParts(environment);
            if (parts.isEmpty()) {
                return false;
            }

            Part part = UploadHelper.getPartForName(parts, name);
            if (part == null) {
                return false;
            }

            long uploadSize = part.getSize();
            long maxUploadSize = SettingsBean.getInstance().getJahiaFileUploadMaxSize();
            if (uploadSize > maxUploadSize) {
                throw new FileSizeLimitExceededException(
                        String.format(
                                "The field %s exceeds its maximum permitted size of %s bytes.",
                                part.getName(),
                                maxUploadSize
                        ),
                        uploadSize,
                        maxUploadSize
                );
            }

            return true;
        } catch (IOException | ServletException e) {
            throw new DataFetchingException("Cannot read parts");
        }
    }

    /**
     * Return the FileItem for the specified part name.
     *
     * @param name        Name of the part
     * @param environment The DataFetchingEnvironment
     * @return The FileItem matching the specified name
     */
    public static Part getFileUpload(String name, DataFetchingEnvironment environment) {
        try {
            List<Part> parts = getParts(environment);
            if (parts.isEmpty()) {
                throw new GqlJcrWrongInputException("Must use multipart request");
            }

            Part part = getPartForName(parts, name);
            if (part == null) {
                throw new GqlJcrWrongInputException("Must send file as multipart request for " + name);
            }
            return part;
        } catch (IOException | ServletException e) {
            throw new DataFetchingException("Cannot read parts");
        }
    }

    private static List<Part> getParts(DataFetchingEnvironment environment) throws IOException, ServletException {
        List<Part> parts = ContextUtil.getHttpServletRequest(environment.getGraphQlContext()).getParts().stream()
                .filter(part -> part.getContentType() != null)
                .collect(Collectors.toList());
        return parts;
    }

    /**
     * @return File upload Part that matches name from context file parts,
     * or null if no Part exists or if there are more than one part that has the same filename
     */
    private static Part getPartForName(List<Part> parts, String name) {
        parts = parts.stream()
                .filter(part -> name.equals(part.getName()))
                .collect(Collectors.toList());

        return (parts.isEmpty() || parts.size() > 1) ?
                null : parts.get(0);
    }

}
