/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.upload;

import graphql.schema.DataFetchingEnvironment;
import graphql.kickstart.servlet.context.GraphQLServletContext;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.settings.SettingsBean;

import javax.servlet.http.Part;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Get FileItem from multipart request.
 */
public class UploadHelper {

    /**
     * Check if the specified value matches a part in the request.
     * @param name Name of the part
     * @param environment The DataFetchingEnvironment
     * @return true if a FileItem is found
     * @throws FileSizeLimitExceededException  if the file exceeds currently set limit
     */
    public static boolean isValidFileUpload(String name, DataFetchingEnvironment environment) throws FileSizeLimitExceededException {
        GraphQLServletContext context = environment.getContext();
        if (context.getParts().isEmpty()) {
            return false;
        }

        Part part = UploadHelper.getPartForFilename(context, name);
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
    }

    /**
     * Return the FileItem for the specified part name.
     * @param name Name of the part
     * @param environment The DataFetchingEnvironment
     * @return The FileItem matching the specified name
     */
    public static Part getFileUpload(String name, DataFetchingEnvironment environment) {
        if (!(environment.getContext() instanceof GraphQLServletContext)) {
            throw new GqlJcrWrongInputException("Not a servlet context");
        }

        GraphQLServletContext context = environment.getContext();
        if (context.getParts().isEmpty()) {
            throw new GqlJcrWrongInputException("Must use multipart request");
        }

        Part part = getPartForFilename(context, name);
        if (part == null) {
            throw new GqlJcrWrongInputException("Must send file as multipart request for " + name);
        }
        return part;
    }

    /**
     *
     * @param context
     * @param filename
     * @return file upload Part that matches filename, or null if no Part exists or
     * if there are more than one part that has the same filename
     */
    private static Part getPartForFilename(GraphQLServletContext context, String filename) {
        List<Part> parts = context.getFileParts()
                .stream()
                .filter(part -> filename.equals(part.getName()))
                .collect(Collectors.toList());

        return (parts.isEmpty() || parts.size() > 1) ?
                null : parts.get(0);
    }

}
