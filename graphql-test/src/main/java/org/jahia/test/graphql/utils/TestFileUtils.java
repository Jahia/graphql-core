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
