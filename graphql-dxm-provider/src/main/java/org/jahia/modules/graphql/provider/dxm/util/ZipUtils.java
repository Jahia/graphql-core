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
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.value.BinaryImpl;
import org.apache.tika.Tika;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRFileContent;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.zip.ZipEntryCharsetDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
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
    private static final Tika TIKA = new Tika();
    static final int BUFFER = 512;
    static final long MAX_SIZE = 0x6400000; // Max size of unzipped data, 100MB
    static final int MAX_ENTRIES = 1024;      // Max number of files

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

    private static String validateFilename(String filename) throws java.io.IOException {
        String canonicalPath = new File(filename).getCanonicalPath();
        String canonicalID = new File(".").getCanonicalPath();

        if (canonicalPath.startsWith(canonicalID) && canonicalPath.length() > canonicalID.length()) {
            return canonicalPath.substring(canonicalID.length() + 1);
        } else {
            throw new IllegalStateException("File is outside extraction target directory.");
        }
    }

    /**
     * method to unzip a zip file with all its tree
     *
     * @param dest    destination node (folder)
     * @param zipFile zip file to unzip
     */
    public static void unzip(JCRNodeWrapper dest, JCRNodeWrapper zipFile) {
        long maxSize = SettingsBean.getInstance().getLong("zipFile.maxSize", MAX_SIZE);
        int maxEntries = SettingsBean.getInstance().getInt("zipFile.maxEntriesCount", MAX_ENTRIES);

        JCRFileContent fileContent = zipFile.getFileContent();
        Charset charset = ZipEntryCharsetDetector.detect(fileContent);

        File zipTmpFile = null;
        ZipFile zip = null;
        try (InputStream is = fileContent.downloadFile()) {
            zipTmpFile = File.createTempFile("zipfile", ".zip");
            FileUtils.copyInputStreamToFile(is, zipTmpFile);
            zip = new ZipFile(zipTmpFile, charset);
            int entries = 0;
            long total = 0;

            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                try {
                    JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);

                    String filename = zipEntry.getName().replace('\\', '/');
                    validateFilename(filename);
                    if (filename.endsWith("/")) {
                        filename = filename.substring(0, filename.length() - 1);
                    }
                    int endIndex = filename.lastIndexOf('/');
                    String parentName = dest.getPath();
                    if (endIndex > -1) {
                        parentName += "/" + filename.substring(0, endIndex);
                        filename = filename.substring(endIndex + 1);
                    }

                    JCRNodeWrapper target = ensureDir(parentName, currentUserSession);

                    if (zipEntry.isDirectory()) {
                        String folderName = JCRContentUtils.escapeLocalNodeName(filename);
                        if (!target.hasNode(folderName)) {
                            target.addNode(folderName, Constants.JAHIANT_FOLDER);
                        }
                    } else {
                        byte data[] = new byte[BUFFER];
                        int count;
                        // Write the files to the disk, but ensure that the filename is valid,
                        // and that the file is not insanely big
                        File unzippedTmpFile = File.createTempFile("unzipped", "");
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzippedTmpFile), BUFFER);
                        InputStream zis = zip.getInputStream(zipEntry);
                        while (total + BUFFER <= maxSize && (count = zis.read(data, 0, BUFFER)) != -1) {
                            bos.write(data, 0, count);
                            total += count;
                        }
                        bos.close();
                        entries++;
                        if (entries > maxEntries) {
                            throw new IllegalStateException("Too many files to unzip.");
                        }
                        if (total + BUFFER > maxSize) {
                            throw new IllegalStateException("File being unzipped is too big.");
                        }

                        try (InputStream inputStream = Files.newInputStream(unzippedTmpFile.toPath())) {
                            target.uploadFile(filename, inputStream, getMimeType(zipEntry.getName(), inputStream));
                        } finally {
                            Files.delete(unzippedTmpFile.toPath());
                        }
                    }
                } catch (IOException | RepositoryException e) {
                    logger.error("Failed to process zip entry during unzip", e);
                }
            }
        } catch (IOException e) {
            throw new DataFetchingException(e);
        } finally {
            FileUtils.deleteQuietly(zipTmpFile);
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

    public static String getMimeType(String name, InputStream inputStream) throws IOException {
        String mimeType = TIKA.detect(name);
        if ((mimeType == null || StringUtils.equals("application/octet-stream", mimeType)) && inputStream != null) {
            mimeType = TIKA.detect(inputStream);
        }
        if (mimeType == null) {
            logger.warn("Unable to resolve mime type for file {}", name);
        }
        return mimeType;
    }

    private static JCRNodeWrapper ensureDir(String path, JCRSessionWrapper currentUserSession) throws RepositoryException {
        try {
            return currentUserSession.getNode(JCRContentUtils.escapeNodePath(path));
        } catch (RepositoryException e) {
            if (e instanceof PathNotFoundException || e.getCause() != null && e.getCause() instanceof MalformedPathException) {
                int endIndex = path.lastIndexOf('/');
                if (endIndex == -1) {
                    return null;
                }
                JCRNodeWrapper parentDir = ensureDir(path.substring(0, endIndex), currentUserSession);
                if (parentDir == null) {
                    return null;
                }
                return parentDir.createCollection(JCRContentUtils.escapeLocalNodeName(path.substring(path.lastIndexOf('/') + 1)));
            } else {
                throw e;
            }
        }
    }

}
