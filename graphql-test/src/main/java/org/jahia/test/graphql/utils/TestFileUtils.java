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
package org.jahia.test.graphql.utils;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * File utilities for upload tests
 */
public class TestFileUtils {

    public static Part getFilePart(String fieldName, String fileName, String fileContent) throws IOException {
        DiskFileItem diskFileItem = new DiskFileItem(fieldName, "text/plain",
                false, fileName, 100, null);

        OutputStream out = diskFileItem.getOutputStream();
        IOUtils.write(fileContent.getBytes(StandardCharsets.UTF_8), out);
        IOUtils.close(out);

        return toPart(diskFileItem);
    }

    public static Part toPart(FileItem fileItem) {
        return new Part() {
            @Override public InputStream getInputStream() throws IOException {
                return fileItem.getInputStream();
            }

            @Override public String getContentType() {
                return fileItem.getContentType();
            }

            @Override public String getName() {
                return fileItem.getFieldName();
            }

            @Override public long getSize() {
                return fileItem.getSize();
            }

            @Override public void write(String s) throws IOException {
                // auto-generated; do nothing
            }

            @Override public void delete() throws IOException {
                fileItem.delete();
            }

            @Override public String getHeader(String s) {
                return fileItem.getHeaders().getHeader(s);
            }

            @Override public Collection<String> getHeaders(String s) {
                return null;
            }

            @Override public Collection<String> getHeaderNames() {
                return null;
            }
        };
    }

}
