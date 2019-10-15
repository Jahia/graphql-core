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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRFileContent;

import javax.jcr.RepositoryException;
import java.io.*;
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
     */
    public static void addToZip(List<JCRNodeWrapper> nodes, JCRNodeWrapper file) {
        InputStream is = null;
        String filename = file.getName();
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ZipOutputStream zout = new ZipOutputStream(fos);

            //if the zip file already exists we transfer all its subfiles into the new zip file
            if (file.hasNode(Constants.JCR_CONTENT) && file.getNode(Constants.JCR_CONTENT).hasProperty(Constants.JCR_DATA)) {
                ZipInputStream zin = new ZipInputStream(file.getFileContent().downloadFile());
                ZipEntry entry = zin.getNextEntry();
                while (entry != null) {
                    zout.putNextEntry(entry);
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = zin.read(bytes)) > 0) {
                        zout.write(bytes, 0, len);
                    }
                    entry = zin.getNextEntry();
                }
                //else, we add mandatory child nodes
            } else {
                file.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
            }
            //now we add the new files/directories
            for (JCRNodeWrapper node : nodes) {
                if (node.isNodeType(Constants.JAHIANT_FILE)) {
                    zip(node, zout);
                } else if (node.isNodeType(Constants.JAHIANT_FOLDER)){
                    JCRNodeIteratorWrapper it = node.getNodes();
                    while (it.hasNext()) {
                        JCRNodeWrapper n = ((JCRNodeWrapper)it.next());
                        zip(n, zout);
                    }
                }
            }
            is = new BufferedInputStream(new FileInputStream(filename));
            zout.close();
            //put the new zip file into jcr:data
            file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_DATA, new BinaryImpl(is));
            file.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_MIMETYPE, "application/zip");
        } catch (IOException | RepositoryException e) {
            throw new DataFetchingException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static void zip(JCRNodeWrapper node, ZipOutputStream zipOutputStream) {
        try {
        JCRFileContent fileContent = node.getFileContent();
        InputStream inputStream = fileContent.downloadFile();
        ZipEntry zipEntry = new ZipEntry(node.getName());

            zipOutputStream.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = inputStream.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }

        zipOutputStream.closeEntry();
        inputStream.close();
        } catch (IOException e) {
            throw new DataFetchingException(e);
        }
    }
}
