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
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.io.*;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * helper methods to zip folders/files
 *
 * @author yousria
 */
public class ZipUtils {

    /**
     * method to zip a file / several files / folder or add files to an existing zip file (without duplicates)
     *
     * @param nodes list of nodes to be included in the new zip file (only files or folders)
     * @param file  the zip file
     */
    public static void addToZip(List<JCRNodeWrapper> nodes, JCRNodeWrapper file) {
        InputStream is = null;
        String filename = file.getName();
        File tmp = null;
        ZipInputStream zin = null;
        ZipOutputStream zout = null;
        try {
            tmp = File.createTempFile(filename, "");
            zout = new ZipOutputStream(new FileOutputStream(tmp));

            //if the zip file already exists we transfer all its subfiles into the new zip file
            if (file.hasNode(Constants.JCR_CONTENT) && file.getNode(Constants.JCR_CONTENT).hasProperty(Constants.JCR_DATA)) {
                zin = new ZipInputStream(file.getFileContent().downloadFile());
                ZipEntry entry = zin.getNextEntry();
                while (entry != null) {
                    zout.putNextEntry(entry);
                    IOUtils.copy(zin, zout);
                    entry = zin.getNextEntry();
                }
                zin.close();
                //else, we add mandatory child nodes
            } else {
                file.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
            }
            //now we add the new files/directories
            for (JCRNodeWrapper node : nodes) {
                zip(node, zout, "");
            }
            zout.close();
            is = new BufferedInputStream(new FileInputStream(tmp));
            //put the new zip file into jcr:data
            file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_DATA, new BinaryImpl(is));
            file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_MIMETYPE, "application/zip");
        } catch (IOException | RepositoryException e) {
            IOUtils.closeQuietly(zin);
            IOUtils.closeQuietly(zout);
            throw new DataFetchingException(e);
        } finally {
            IOUtils.closeQuietly(is);
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
        ZipInputStream zis = null;
        String mimeType;
        try {
            zis = new ZipInputStream(zipFile.getFileContent().downloadFile());
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {

                if (entry.isDirectory()) {
                    //if the entry is a directory, create it to build the whole tree
                    dest.addNode(entry.getName(), Constants.JAHIANT_FOLDER);

                } else {

                    tmp = File.createTempFile(entry.getName(), "");
                    try (FileOutputStream fos = new FileOutputStream(tmp)) {
                        IOUtils.copy(zis, fos);
                    }

                    try (InputStream is1 = new FileInputStream(tmp)) {
                        mimeType = getMimeType(entry.getName(), is1);
                    }

                    try(InputStream is2 = new FileInputStream(tmp)){
                        dest.uploadFile(entry.getName(), is2, mimeType);
                    }

                    FileUtils.deleteQuietly(tmp);
                }
                entry = zis.getNextEntry();

            }
            zis.closeEntry();
            zis.close();
        } catch (IOException | RepositoryException e) {
            IOUtils.closeQuietly(zis);
            FileUtils.deleteQuietly(tmp);
            throw new DataFetchingException(e);
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
                parent = node.getName() + "/";
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

    private static String getMimeType(String name, InputStream inputStream) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(name);
        if (mimeType == null) {
            mimeType = URLConnection.guessContentTypeFromStream(inputStream);
        }
        return mimeType;
    }
}
