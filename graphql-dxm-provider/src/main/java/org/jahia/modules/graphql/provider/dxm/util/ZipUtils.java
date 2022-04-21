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
package org.jahia.modules.graphql.provider.dxm.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.*;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * helper methods to zip folders/files
 *
 * @author yousria
 */
public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    private ZipUtils() {
    }

    /**
     * method to zip a file / several files / folder or add files to an existing zip file (without duplicates)
     *
     * @param nodes list of nodes to be included in the new zip file (only files or folders)
     * @param file  the zip file
     */
    public static void addToZip(List<JCRNodeWrapper> nodes, JCRNodeWrapper file) {
        String filename = file.getName();
        File tmp = null;
        try {
            tmp = File.createTempFile(filename, "");

            try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmp))) {
                //if the zip file already exists we transfer all its subfiles into the new zip file
                if (file.hasNode(Constants.JCR_CONTENT) && file.getNode(Constants.JCR_CONTENT).hasProperty(Constants.JCR_DATA)) {
                    try (ZipInputStream zin = new ZipInputStream(file.getFileContent().downloadFile())) {
                        ZipEntry entry = zin.getNextEntry();
                        while (entry != null) {
                            zout.putNextEntry(entry);
                            IOUtils.copy(zin, zout);
                            entry = zin.getNextEntry();
                        }
                    }
                } else {
                    //else, we add mandatory child nodes
                    file.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
                }
                //now we add the new files/directories
                for (JCRNodeWrapper node : nodes) {
                    zip(node, zout, "");
                }
            }

            //put the new zip file into jcr:data
            try (InputStream is = new BufferedInputStream(new FileInputStream(tmp))) {
                file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_DATA, new BinaryImpl(is));
            }
            file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_MIMETYPE, "application/zip");
        } catch (IOException | RepositoryException e) {
            throw new DataFetchingException(e);
        } finally {
            FileUtils.deleteQuietly(tmp);
        }
    }

    /**
     * method to unzip a zip file with all its tree
     *
     * @param dest    destination node (folder)
     * @param zipFile zip file to unzip
     */
    public static void unzip(JCRNodeWrapper dest, JCRNodeWrapper zipFile) {
        File tmp = null;
        ZipFile zip = null;
        try (InputStream is = zipFile.getFileContent().downloadFile()) {
            tmp = File.createTempFile(UUID.randomUUID() + ".zip", "");
            FileUtils.copyInputStreamToFile(is, tmp);
            zip = new ZipFile(tmp);
            File finalTmp = tmp;
            ZipFile finalZip = zip;
            zip.stream().forEach(entry -> {
                try {
                    if (entry.isDirectory()) {
                        //if the entry is a directory, create it to build the whole tree
                        if (!JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE).nodeExists(dest.getPath() + "/" + entry.getName())) {
                            dest.addNode(entry.getName(), Constants.JAHIANT_FOLDER);
                        }
                    } else {
                        String name = entry.getName();
                        if(name.lastIndexOf("/") > 0) {
                            String parentName = name.substring(0, name.lastIndexOf("/"));
                            //if the entry has a parent directory and this one is not listed in zip entries we re-create it here to avoid a PathNotFound exception
                            if (!JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE).nodeExists(dest.getPath() + "/" + parentName)) {
                                dest.addNode(parentName, Constants.JAHIANT_FOLDER);
                            }
                            dest.getNode(name.substring(0, name.lastIndexOf("/"))).uploadFile(name, finalZip.getInputStream(entry), getMimeType(entry.getName(), finalTmp));
                        } else {
                            dest.uploadFile(name, finalZip.getInputStream(entry), getMimeType(entry.getName(), finalTmp));
                        }
                    }
                } catch (IOException | RepositoryException e) {
                    logger.error("Failed to process zip entry during unzip", e);
                }
            });
        } catch (IOException e) {
            throw new DataFetchingException(e);
        } finally {
            FileUtils.deleteQuietly(tmp);
            try {
                if (zip != null) {
                    // Closes streams opened in the foreach
                    zip.close();
                }
            } catch (IOException e) {
                logger.error("Failed to close zip stream", e);
            }
        }
    }

    /**
     * @param node   the node (file) to zip
     * @param zout   zipOutputStream of the zip file
     * @param parent empty string when files are on root path, parent name instead
     * @throws RepositoryException
     */
    private static void zip(JCRNodeWrapper node, ZipOutputStream zout, String parent) throws RepositoryException {
        try {
            if (node.isNodeType(Constants.JAHIANT_FILE)) {
                try (InputStream inputStream = node.getFileContent().downloadFile()) {
                    zout.putNextEntry(new ZipEntry(parent + node.getName()));
                    IOUtils.copy(inputStream, zout);
                    zout.closeEntry();
                }
            } else if (node.isNodeType(Constants.JAHIANT_FOLDER)) {
                parent += node.getName() + "/";
                zout.putNextEntry(new ZipEntry(parent));
                JCRNodeIteratorWrapper it = node.getNodes();
                while (it.hasNext()) {
                    JCRNodeWrapper n = ((JCRNodeWrapper) it.next());
                    zip(n, zout, parent);
                }
            }
        } catch (IOException e) {
            throw new DataFetchingException(e);
        }
    }

    private static String getMimeType(String name, File tmp) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(name);
        if (mimeType == null) {
            try (InputStream inputStream = new FileInputStream(tmp)) {
                mimeType = URLConnection.guessContentTypeFromStream(inputStream);
            }
        }
        return mimeType;
    }
}
