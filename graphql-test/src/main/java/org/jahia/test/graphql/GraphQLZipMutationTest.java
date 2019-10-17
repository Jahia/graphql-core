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
package org.jahia.test.graphql;

import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test class for zip mutations on files
 *
 * @author yousria
 */
public class GraphQLZipMutationTest extends GraphQLTestSupport {

    private static <T> T inJcr(JCRCallback<T> callback) throws Exception {
        return inJcr(callback, null);
    }

    private static <T> T inJcr(JCRCallback<T> callback, Locale locale) throws Exception {
        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE,
                locale != null ? locale : Locale.ENGLISH, callback);
    }

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @Before
    public void setUp() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper testFolder = session.getRootNode().addNode("testFolder", Constants.JAHIANT_FOLDER);
            JCRNodeWrapper folderA = testFolder.addNode("folderA", Constants.JAHIANT_FOLDER);
            JCRNodeWrapper folderB = testFolder.addNode("folderB", Constants.JAHIANT_FOLDER);
            JCRNodeWrapper fileA = folderA.addNode("fileA.txt", Constants.JAHIANT_FILE);
            fileA.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
            fileA.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_DATA, new BinaryImpl(new byte[1024]));
            fileA.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_MIMETYPE, "text/plain");
            JCRNodeWrapper fileB = folderB.addNode("fileB.txt", Constants.JAHIANT_FILE);
            fileB.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
            fileB.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_DATA, new BinaryImpl(new byte[1024]));
            fileB.getNode(Constants.JCR_CONTENT).setProperty(Constants.JCR_MIMETYPE, "text/plain");
            session.save();
            return null;
        });
    }

    @After
    public void tearDown() throws Exception {
        inJcr(session -> {
            if (session.nodeExists("/testFolder")) {
                session.getNode("/testFolder").remove();
                session.save();
            }
            return null;
        });
        JCRSessionFactory.getInstance().closeAllSessions();
    }


    @Test
    public void shouldZipFile() throws Exception {
        JSONObject result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/testFolder\", name:\"zipFile.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/testFolder/folderA/fileA.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getJSONObject("zip").getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertTrue(session.getNode("/testFolder").hasNode("zipFile.zip"));
            try {
                try (ZipInputStream zis = new ZipInputStream(session.getNode("/testFolder/zipFile.zip").getFileContent().downloadFile())) {
                    ZipEntry entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("fileA.txt"));
                }
            } catch (IOException e) {
                e.getStackTrace();
            }
            return null;
        });
    }

    @Test
    public void shouldZipFolder() throws Exception {
        JSONObject result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/testFolder\", name:\"zipFolder.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/testFolder/folderA\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getJSONObject("zip").getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertTrue(session.getNode("/testFolder").hasNode("zipFolder.zip"));
            try {
                try (ZipInputStream zis = new ZipInputStream(session.getNode("/testFolder/zipFolder.zip").getFileContent().downloadFile())) {
                    ZipEntry entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("folderA/"));
                }
            } catch (IOException e) {
                e.getStackTrace();
            }
            return null;
        });
    }

    @Test
    public void shouldAddFileToZip() throws Exception {
        executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/testFolder\", name:\"zipFile.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/testFolder/folderA/fileA.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );

        JSONObject result = executeQuery(
                "mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/testFolder/zipFile.zip\") {"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/testFolder/folderB/fileB.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("zip").getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertTrue(session.getNode("/testFolder").hasNode("zipFile.zip"));
            try {
                try (ZipInputStream zis = new ZipInputStream(session.getNode("/testFolder/zipFile.zip").getFileContent().downloadFile())) {
                    ZipEntry entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("fileA.txt"));
                    entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("fileB.txt"));
                }
            } catch (IOException e) {
                e.getStackTrace();
            }
            return null;
        });
    }

    @Test
    public void shouldZipMultipleFiles() throws Exception {
        JSONObject result = executeQuery(
                "mutation {\n"
                        + "  jcr {\n"
                        + "    addNode(parentPathOrId:\"/testFolder\", name:\"zipFiles.zip\", primaryNodeType:\"jnt:file\") {\n"
                        + "      zip {\n"
                        + "        addToZip(pathsOrIds:[\"/testFolder/folderB/fileB.txt\", \"/testFolder/folderA/fileA.txt\"])\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("addNode").getJSONObject("zip").getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertTrue(session.getNode("/testFolder").hasNode("zipFiles.zip"));
            try {
                try (ZipInputStream zis = new ZipInputStream(session.getNode("/testFolder/zipFiles.zip").getFileContent().downloadFile())) {
                    ZipEntry entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("fileB.txt"));
                    entry = zis.getNextEntry();
                    Assert.assertTrue(entry.getName().equals("fileA.txt"));
                }
            } catch (IOException e) {
                e.getStackTrace();
            }
            return null;
        });
    }

    @Test
    public void shouldUnzipFile() throws Exception {
        executeQuery("mutation {\n"
                        + "  jcr {\n"
                        + "    addNode(parentPathOrId:\"/testFolder\", name:\"zipFiles.zip\", primaryNodeType:\"jnt:file\") {\n"
                        + "      zip {\n"
                        + "        addToZip(pathsOrIds:[\"/testFolder/folderB/fileB.txt\", \"/testFolder/folderA/fileA.txt\"])\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}"
        );
        JSONObject result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/testFolder/zipFiles.zip\") {\n"
                + "      zip {\n"
                + "        unzip(path:\"/testFolder\")\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("zip").getBoolean("unzip"));
        inJcr(session -> {
            Assert.assertTrue(session.nodeExists("/testFolder/fileA.txt"));
            Assert.assertTrue(session.nodeExists("/testFolder/fileB.txt"));
            return null;
        });
        executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/testFolder\", name:\"test.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/testFolder/folderB\", \"/testFolder/folderA\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );

        result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/testFolder/test.zip\") {\n"
                + "      zip {\n"
                + "        unzip(path:\"/testFolder/folderA\")\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        Assert.assertTrue(result.getJSONObject("data").getJSONObject("jcr").getJSONObject("mutateNode").getJSONObject("zip").getBoolean("unzip"));
        inJcr(session -> {
            Assert.assertTrue(session.nodeExists("/testFolder/folderA/folderA"));
            Assert.assertTrue(session.nodeExists("/testFolder/folderA/folderB"));
            Assert.assertTrue(session.nodeExists("/testFolder/folderA/folderA/fileA.txt"));
            Assert.assertTrue(session.nodeExists("/testFolder/folderA/folderB/fileB.txt"));
            return null;
        });

    }

}
