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

import org.jahia.api.Constants;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.tags.TaggingService;
import org.jahia.test.TestHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GraphQLTagManagerTest extends GraphQLTestSupport {

    private static final String SITE_KEY = "graphql_tag_manager";
    private static final String OTHER_SITE_KEY = "graphql_tag_manager_other";
    private static final String SPECIAL_TAG = "ù^$ùç_\"(_çt\"à'çr(";

    private TaggingService taggingService;
    private String alphaNodeId;
    private String betaNodeId;
    private String gammaNodeId;

    @BeforeClass
    public static void oneTimeSetup() {
        GraphQLTestSupport.init();
    }

    @Before
    public void setUp() throws Exception {
        taggingService = BundleUtils.getOsgiService(TaggingService.class, null);
        removeSites();
        createAndSeedSites();
    }

    @After
    public void tearDown() throws Exception {
        JCRSessionFactory.getInstance().closeAllSessions();
        removeSites();
    }

    @Test
    public void shouldReturnManagedTagsSortedByNameAndPaginated() throws Exception {
        JSONObject ascResult = executeQuery("{ admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " tags(sortBy: NAME, sortOrder: ASC) {"
                + "  nodes { name occurrences }"
                + "  pageInfo { totalCount nodesCount hasPreviousPage hasNextPage }"
                + " }"
                + "} } } }");

        JSONObject connection = ascResult.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("tags");
        JSONArray nodes = connection.getJSONArray("nodes");

        Assert.assertEquals(3, nodes.length());
        Assert.assertEquals("alpha-tag", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals(1, nodes.getJSONObject(0).getLong("occurrences"));
        Assert.assertEquals("beta-tag", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals(2, nodes.getJSONObject(1).getLong("occurrences"));
        Assert.assertEquals("gamma-tag", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals(3, nodes.getJSONObject(2).getLong("occurrences"));

        JSONObject pageInfo = connection.getJSONObject("pageInfo");
        Assert.assertEquals(3, pageInfo.getInt("totalCount"));
        Assert.assertEquals(3, pageInfo.getInt("nodesCount"));
        Assert.assertFalse(pageInfo.getBoolean("hasPreviousPage"));
        Assert.assertFalse(pageInfo.getBoolean("hasNextPage"));

        JSONObject pagedResult = executeQuery("{ admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " tags(sortBy: NAME, sortOrder: DESC, limit: 1, offset: 1) {"
                + "  nodes { name occurrences }"
                + "  pageInfo { totalCount nodesCount hasPreviousPage hasNextPage }"
                + " }"
                + "} } } }");

        JSONObject pagedConnection = pagedResult.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("tags");
        JSONArray pagedNodes = pagedConnection.getJSONArray("nodes");
        Assert.assertEquals(1, pagedNodes.length());
        Assert.assertEquals("beta-tag", pagedNodes.getJSONObject(0).getString("name"));

        JSONObject pagedPageInfo = pagedConnection.getJSONObject("pageInfo");
        Assert.assertEquals(3, pagedPageInfo.getInt("totalCount"));
        Assert.assertEquals(1, pagedPageInfo.getInt("nodesCount"));
        Assert.assertTrue(pagedPageInfo.getBoolean("hasPreviousPage"));
        Assert.assertTrue(pagedPageInfo.getBoolean("hasNextPage"));
    }

    @Test
    public void shouldReturnManagedTagsSortedByOccurrences() throws Exception {
        JSONObject result = executeQuery("{ admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " tags(sortBy: OCCURRENCES, sortOrder: DESC) {"
                + "  nodes { name occurrences }"
                + " }"
                + "} } } }");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("tags").getJSONArray("nodes");

        Assert.assertEquals("gamma-tag", nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals(3, nodes.getJSONObject(0).getLong("occurrences"));
        Assert.assertEquals("beta-tag", nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals(2, nodes.getJSONObject(1).getLong("occurrences"));
        Assert.assertEquals("alpha-tag", nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals(1, nodes.getJSONObject(2).getLong("occurrences"));
    }

    @Test
    public void shouldReturnTaggedContentScopedToTheRequestedSite() throws Exception {
        JSONObject result = executeQuery("{ admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " taggedContent(tag: \"gamma-tag\", limit: 10, offset: 0) {"
                + "  nodes { uuid path displayName primaryNodeType { name icon } }"
                + "  pageInfo { totalCount nodesCount }"
                + " }"
                + "} } } }");

        JSONObject connection = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("taggedContent");
        JSONArray nodes = connection.getJSONArray("nodes");

        Assert.assertEquals(3, nodes.length());
        Assert.assertEquals("/sites/" + SITE_KEY + "/tags/alpha", nodes.getJSONObject(0).getString("path"));
        Assert.assertEquals("/sites/" + SITE_KEY + "/tags/beta", nodes.getJSONObject(1).getString("path"));
        Assert.assertEquals("/sites/" + SITE_KEY + "/tags/gamma", nodes.getJSONObject(2).getString("path"));
        Assert.assertEquals("jnt:contentList", nodes.getJSONObject(0).getJSONObject("primaryNodeType").getString("name"));
        Assert.assertTrue(nodes.getJSONObject(0).getJSONObject("primaryNodeType").has("icon"));

        JSONObject pageInfo = connection.getJSONObject("pageInfo");
        Assert.assertEquals(3, pageInfo.getInt("totalCount"));
        Assert.assertEquals(3, pageInfo.getInt("nodesCount"));
    }

    @Test
    public void shouldKeepTagSuggestQueryUnchanged() throws Exception {
        JSONObject result = executeQuery("{ tag { suggest(prefix: \"ga\", limit: 10, startPath: \"/sites/" + SITE_KEY + "\", minCount: 1, offset: 0, sortByCount: true) {"
                + " name occurences"
                + "} } }");

        JSONArray suggestions = result.getJSONObject("data").getJSONObject("tag").getJSONArray("suggest");
        Assert.assertEquals(1, suggestions.length());
        Assert.assertEquals("gamma-tag", suggestions.getJSONObject(0).getString("name"));
        Assert.assertEquals(3, suggestions.getJSONObject(0).getLong("occurences"));
    }

    @Test
    public void shouldHandleSpecialCharacterTagsForUsageRenameAndDelete() throws Exception {
        String escapedTag = escapeGraphQLString(SPECIAL_TAG);

        JSONObject usagesResult = executeQuery("{ admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " taggedContent(tag: \"" + escapedTag + "\", limit: 10, offset: 0) {"
                + "  nodes { uuid path }"
                + "  pageInfo { totalCount nodesCount }"
                + " }"
                + "} } } }");

        JSONObject usagesConnection = usagesResult.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("taggedContent");
        Assert.assertEquals(1, usagesConnection.getJSONObject("pageInfo").getInt("totalCount"));
        Assert.assertEquals("/sites/" + SITE_KEY + "/tags/gamma", usagesConnection.getJSONArray("nodes").getJSONObject(0).getString("path"));

        JSONObject renameResult = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " renameTag(tag: \"" + escapedTag + "\", newName: \"special-renamed\") {"
                + "  workspaceResults { workspace processedCount errors { path } }"
                + " }"
                + "} } } }");

        JSONArray renameWorkspaceResults = renameResult.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("renameTag").getJSONArray("workspaceResults");
        Assert.assertEquals(1, renameWorkspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(1, renameWorkspaceResults.getJSONObject(1).getInt("processedCount"));
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma", "gamma-tag", "special-renamed");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma", "gamma-tag", "special-renamed");

        JSONObject deleteResult = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " deleteTag(tag: \"special-renamed\") {"
                + "  workspaceResults { workspace processedCount errors { path } }"
                + " }"
                + "} } } }");

        JSONArray deleteWorkspaceResults = deleteResult.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("deleteTag").getJSONArray("workspaceResults");
        Assert.assertEquals(1, deleteWorkspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(1, deleteWorkspaceResults.getJSONObject(1).getInt("processedCount"));
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma", "gamma-tag");
    }

    @Test
    public void shouldRenameTagInEditAndLive() throws Exception {
        JSONObject result = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " renameTag(tag: \"beta-tag\", newName: \"beta-renamed\") {"
                + "  tag"
                + "  workspaceResults { workspace processedCount errors { path displayableName } }"
                + " }"
                + "} } } }");

        JSONArray workspaceResults = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("renameTag").getJSONArray("workspaceResults");
        Assert.assertEquals(2, workspaceResults.length());
        Assert.assertEquals(2, workspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(0, workspaceResults.getJSONObject(0).getJSONArray("errors").length());
        Assert.assertEquals(2, workspaceResults.getJSONObject(1).getInt("processedCount"));
        Assert.assertEquals(0, workspaceResults.getJSONObject(1).getJSONArray("errors").length());

        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-renamed", "gamma-tag");
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-renamed", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-renamed", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-renamed", "gamma-tag");
    }

    @Test
    public void shouldDeleteTagInEditAndLive() throws Exception {
        JSONObject result = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " deleteTag(tag: \"gamma-tag\") {"
                + "  tag"
                + "  workspaceResults { workspace processedCount errors { path } }"
                + " }"
                + "} } } }");

        JSONArray workspaceResults = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("deleteTag").getJSONArray("workspaceResults");
        Assert.assertEquals(3, workspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(3, workspaceResults.getJSONObject(1).getInt("processedCount"));

        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag");
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-tag");
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/gamma");
    }

    @Test
    public void shouldDeleteTagOnNodeInEditAndLive() throws Exception {
        JSONObject result = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " deleteTagOnNode(tag: \"beta-tag\", nodeId: \"" + betaNodeId + "\") {"
                + "  tag"
                + "  nodeId"
                + "  workspaceResults { workspace processedCount errors { path } }"
                + " }"
                + "} } } }");

        JSONArray workspaceResults = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("deleteTagOnNode").getJSONArray("workspaceResults");
        Assert.assertEquals(1, workspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(1, workspaceResults.getJSONObject(1).getInt("processedCount"));

        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag", "gamma-tag");
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "gamma-tag");
    }

    @Test
    public void shouldRenameTagOnNodeInEditAndLive() throws Exception {
        JSONObject result = executeQuery("mutation { admin { jahia { tagManager(siteKey: \"" + SITE_KEY + "\") {"
                + " renameTagOnNode(tag: \"beta-tag\", newName: \"beta-local\", nodeId: \"" + betaNodeId + "\") {"
                + "  tag"
                + "  nodeId"
                + "  workspaceResults { workspace processedCount errors { path } }"
                + " }"
                + "} } } }");

        JSONArray workspaceResults = result.getJSONObject("data").getJSONObject("admin").getJSONObject("jahia")
                .getJSONObject("tagManager").getJSONObject("renameTagOnNode").getJSONArray("workspaceResults");
        Assert.assertEquals(1, workspaceResults.getJSONObject(0).getInt("processedCount"));
        Assert.assertEquals(1, workspaceResults.getJSONObject(1).getInt("processedCount"));

        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag", "gamma-tag");
        assertTags(Constants.EDIT_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-local", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/alpha", "alpha-tag", "beta-tag", "gamma-tag");
        assertTags(Constants.LIVE_WORKSPACE, "/sites/" + SITE_KEY + "/tags/beta", "beta-local", "gamma-tag");
    }

    private void createAndSeedSites() throws Exception {
        TestHelper.createSite(SITE_KEY);
        TestHelper.createSite(OTHER_SITE_KEY);

        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);
        try {
            JCRNodeWrapper siteNode = session.getNode("/sites/" + SITE_KEY);
            JCRNodeWrapper tagsFolder = siteNode.addNode("tags", "jnt:contentList");
            JCRNodeWrapper alphaNode = tagsFolder.addNode("alpha", "jnt:contentList");
            JCRNodeWrapper betaNode = tagsFolder.addNode("beta", "jnt:contentList");
            JCRNodeWrapper gammaNode = tagsFolder.addNode("gamma", "jnt:contentList");

            taggingService.tag(alphaNode, Arrays.asList("alpha-tag", "beta-tag", "gamma-tag"));
            taggingService.tag(betaNode, Arrays.asList("beta-tag", "gamma-tag"));
            taggingService.tag(gammaNode, Arrays.asList("gamma-tag", SPECIAL_TAG));

            alphaNodeId = alphaNode.getIdentifier();
            betaNodeId = betaNode.getIdentifier();
            gammaNodeId = gammaNode.getIdentifier();

            JCRNodeWrapper otherTagsFolder = session.getNode("/sites/" + OTHER_SITE_KEY).addNode("tags", "jnt:contentList");
            JCRNodeWrapper otherNode = otherTagsFolder.addNode("outside", "jnt:contentList");
            taggingService.tag(otherNode, Collections.singletonList("gamma-tag"));

            session.save();
            JCRPublicationService.getInstance().publishByMainId(siteNode.getIdentifier());
            JCRPublicationService.getInstance().publishByMainId(session.getNode("/sites/" + OTHER_SITE_KEY).getIdentifier());
        } finally {
            session.logout();
        }
    }

    private void removeSites() throws Exception {
        removeSite(Constants.EDIT_WORKSPACE, SITE_KEY);
        removeSite(Constants.EDIT_WORKSPACE, OTHER_SITE_KEY);
        removeSite(Constants.LIVE_WORKSPACE, SITE_KEY);
        removeSite(Constants.LIVE_WORKSPACE, OTHER_SITE_KEY);
    }

    private void removeSite(String workspace, String siteKey) throws Exception {
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, Locale.ENGLISH, null);
        try {
            if (session.nodeExists("/sites/" + siteKey)) {
                session.getNode("/sites/" + siteKey).remove();
                session.save();
            }
        } finally {
            session.logout();
        }
    }

    private void assertTags(String workspace, String path, String... expectedTags) throws Exception {
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, Locale.ENGLISH, null);
        try {
            Assert.assertEquals(Arrays.asList(expectedTags), getTags(session.getNode(path)));
        } finally {
            session.logout();
        }
    }

    private List<String> getTags(JCRNodeWrapper node) throws RepositoryException {
        if (!node.hasProperty("j:tagList")) {
            return Collections.emptyList();
        }

        List<String> tags = new ArrayList<>();
        for (org.jahia.services.content.JCRValueWrapper value : node.getProperty("j:tagList").getValues()) {
            tags.add(value.getString());
        }
        return tags.stream().sorted().collect(Collectors.toList());
    }

    private String escapeGraphQLString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
