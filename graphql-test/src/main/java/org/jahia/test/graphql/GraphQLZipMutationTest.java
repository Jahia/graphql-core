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
package org.jahia.test.graphql;

import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test class for zip mutations on files
 *
 * @author yousria
 */
public class GraphQLZipMutationTest extends GraphQLTestSupport {

    private static final String FOLDER_NAME = "testFolder" + UUID.randomUUID();

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
            JCRNodeWrapper testFolder = session.getRootNode().addNode(FOLDER_NAME, Constants.JAHIANT_FOLDER);
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
            if (session.nodeExists("/"+ FOLDER_NAME)) {
                session.getNode("/"+ FOLDER_NAME).remove();
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
                + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"zipFile.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderA/fileA.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        JSONObject zip = getJSONObjectByPath("data/jcr/addNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", zip);
        Assert.assertEquals("the mutation ended with errors", true, zip.getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertEquals("Zip file was not created", true, session.getNode("/"+ FOLDER_NAME).hasNode("zipFile.zip"));
            checkFilesIntoZip(Arrays.asList("fileA.txt"), session, "zipFile.zip");
            return null;
        });
    }

    @Test
    public void shouldZipFolder() throws Exception {
        JSONObject result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"zipFolder.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderA\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        JSONObject zip = getJSONObjectByPath("data/jcr/addNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", zip);
        Assert.assertEquals("the mutation ended with errors", true, zip.getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertEquals("Zip file was not created", true, session.getNode("/"+ FOLDER_NAME).hasNode("zipFolder.zip"));
            checkFilesIntoZip(Arrays.asList("folderA/"), session, "zipFolder.zip");
            return null;
        });
    }

    @Test
    public void shouldAddFileToZip() throws Exception {
        executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"zipFile.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderA/fileA.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );

        JSONObject result = executeQuery(
                "mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/"+ FOLDER_NAME +"/zipFile.zip\") {"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderB/fileB.txt\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        JSONObject zip = getJSONObjectByPath("data/jcr/mutateNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", zip);
        Assert.assertEquals("mutation ended with errors", true, zip.getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertEquals("Zip file was not created", true, session.getNode("/"+ FOLDER_NAME).hasNode("zipFile.zip"));
            checkFilesIntoZip(Arrays.asList("fileA.txt", "fileB.txt"), session, "zipFile.zip");
            return null;
        });
    }

    @Test
    public void shouldZipMultipleFiles() throws Exception {
        JSONObject result = executeQuery(
                "mutation {\n"
                        + "  jcr {\n"
                        + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"zipFiles.zip\", primaryNodeType:\"jnt:file\") {\n"
                        + "      zip {\n"
                        + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderB/fileB.txt\", \"/"+ FOLDER_NAME +"/folderA/fileA.txt\"])\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}"
        );
        JSONObject zip = getJSONObjectByPath("data/jcr/addNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", zip);
        Assert.assertEquals("mutation ended with errors", true, zip.getBoolean("addToZip"));
        inJcr(session -> {
            Assert.assertEquals("Zip file was not created", true, session.getNode("/"+ FOLDER_NAME).hasNode("zipFiles.zip"));
            checkFilesIntoZip(Arrays.asList("fileB.txt", "fileA.txt"), session, "zipFiles.zip");
            return null;
        });
    }

    @Test
    public void shouldUnzipFile() throws Exception {
        executeQuery("mutation {\n"
                        + "  jcr {\n"
                        + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"zipFiles.zip\", primaryNodeType:\"jnt:file\") {\n"
                        + "      zip {\n"
                        + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderB/fileB.txt\", \"/"+ FOLDER_NAME +"/folderA/fileA.txt\"])\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}"
        );
        JSONObject result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/"+ FOLDER_NAME +"/zipFiles.zip\") {\n"
                + "      zip {\n"
                + "        unzip(path:\"/"+ FOLDER_NAME +"\")\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );
        JSONObject unzip = getJSONObjectByPath("data/jcr/mutateNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", unzip);
        Assert.assertEquals("mutation ended with errors", true, unzip.getBoolean("unzip"));
        inJcr(session -> {
            Assert.assertEquals("file was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/fileA.txt"));
            Assert.assertEquals("file was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/fileB.txt"));
            return null;
        });
        executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    addNode(parentPathOrId:\"/"+ FOLDER_NAME +"\", name:\"test.zip\", primaryNodeType:\"jnt:file\") {\n"
                + "      zip {\n"
                + "        addToZip(pathsOrIds:[\"/"+ FOLDER_NAME +"/folderB\", \"/"+ FOLDER_NAME +"/folderA\"])\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );

        result = executeQuery("mutation {\n"
                + "  jcr {\n"
                + "    mutateNode(pathOrId:\"/"+ FOLDER_NAME +"/test.zip\") {\n"
                + "      zip {\n"
                + "        unzip(path:\"/"+ FOLDER_NAME +"/folderA\")\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}"
        );

        unzip = getJSONObjectByPath("data/jcr/mutateNode/zip", result);
        Assert.assertNotNull("json returned by the mutation is null", unzip);
        Assert.assertEquals("mutation ended with errors", true, unzip.getBoolean("unzip"));
        inJcr(session -> {
            Assert.assertEquals("folder was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/folderA/folderA"));
            Assert.assertEquals("folder was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/folderA/folderB"));
            Assert.assertEquals("file was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/folderA/folderA/fileA.txt"));
            Assert.assertEquals("file was not found at the expected location", true, session.nodeExists("/"+ FOLDER_NAME +"/folderA/folderB/fileB.txt"));
            return null;
        });

    }

    private JSONObject getJSONObjectByPath(String path, JSONObject jsonObject) {
        String[] objects = path.split("/");
        try {
            for (String object : objects) {
                jsonObject = jsonObject.getJSONObject(object);
            }
        } catch (JSONException e) {
            return null;
        }
        return jsonObject;
    }

    private void checkFilesIntoZip(List<String> files, JCRSessionWrapper session, String zipFile) {
        try {
            try (ZipInputStream zis = new ZipInputStream(session.getNode("/" + FOLDER_NAME + "/" + zipFile).getFileContent().downloadFile())) {
                for (String file : files) {
                    ZipEntry entry = zis.getNextEntry();
                    Assert.assertEquals("file/folder is not found into zip", file, entry.getName());
                }
            }
        } catch (IOException | RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

}
