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
    private static final String SPECIAL_RENAMED_TAG = "special-renamed";
    private static final String ALPHA_TAG = "alpha-tag";
    private static final String BETA_TAG = "beta-tag";
    private static final String BETA_RENAMED_TAG = "beta-renamed";
    private static final String BETA_LOCAL_TAG = "beta-local";
    private static final String GAMMA_TAG = "gamma-tag";
    private static final String SITE_ROOT = "/sites/";
    private static final String TAGS_ROOT = "/tags/";
    private static final String TAGS_FOLDER = "tags";
    private static final String ALPHA_NODE_NAME = "alpha";
    private static final String BETA_NODE_NAME = "beta";
    private static final String GAMMA_NODE_NAME = "gamma";
    private static final String OUTSIDE_NODE_NAME = "outside";
    private static final String CONTENT_LIST_TYPE = "jnt:contentList";
    private static final String DATA = "data";
    private static final String ADMIN = "admin";
    private static final String JAHIA = "jahia";
    private static final String TAG_MANAGER = "tagManager";
    private static final String TAGS = "tags";
    private static final String TAGGED_CONTENT = "taggedContent";
    private static final String NODES = "nodes";
    private static final String PAGE_INFO = "pageInfo";
    private static final String TOTAL_COUNT = "totalCount";
    private static final String NODES_COUNT = "nodesCount";
    private static final String OCCURRENCES = "occurrences";
    private static final String WORKSPACE_RESULTS = "workspaceResults";
    private static final String PROCESSED_COUNT = "processedCount";
    private static final String QUERY_PREFIX = "{ admin { jahia { tagManager(siteKey: \"";
    private static final String MUTATION_PREFIX = "mutation { admin { jahia { tagManager(siteKey: \"";
    private static final String QUERY_SUFFIX = "\") {";
    private static final String QUERY_END = "} } } }";

    private TaggingService taggingService;
    private String betaNodeId;

    @BeforeClass
    public static void oneTimeSetup() {
        GraphQLTestSupport.init();
    }

    @Before
    public void setUp() {
        taggingService = BundleUtils.getOsgiService(TaggingService.class, null);
        removeSites();
        createAndSeedSites();
    }

    @After
    public void tearDown() {
        JCRSessionFactory.getInstance().closeAllSessions();
        removeSites();
    }

    @Test
    public void shouldReturnManagedTagsSortedByNameAndPaginated() {
        JSONObject ascResult = executeTagManagerQuery(
                " tags(sortBy: NAME, sortOrder: ASC) {"
                        + "  nodes { name occurrences }"
                        + "  pageInfo { totalCount nodesCount hasPreviousPage hasNextPage }"
                        + " }");

        JSONObject connection = getTagManagerResult(ascResult).getJSONObject(TAGS);
        JSONArray nodes = connection.getJSONArray(NODES);

        Assert.assertEquals(3, nodes.length());
        Assert.assertEquals(ALPHA_TAG, nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals(1, nodes.getJSONObject(0).getLong(OCCURRENCES));
        Assert.assertEquals(BETA_TAG, nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals(2, nodes.getJSONObject(1).getLong(OCCURRENCES));
        Assert.assertEquals(GAMMA_TAG, nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals(3, nodes.getJSONObject(2).getLong(OCCURRENCES));

        JSONObject pageInfo = connection.getJSONObject(PAGE_INFO);
        Assert.assertEquals(3, pageInfo.getInt(TOTAL_COUNT));
        Assert.assertEquals(3, pageInfo.getInt(NODES_COUNT));
        Assert.assertFalse(pageInfo.getBoolean("hasPreviousPage"));
        Assert.assertFalse(pageInfo.getBoolean("hasNextPage"));

        JSONObject pagedResult = executeTagManagerQuery(
                " tags(sortBy: NAME, sortOrder: DESC, limit: 1, offset: 1) {"
                        + "  nodes { name occurrences }"
                        + "  pageInfo { totalCount nodesCount hasPreviousPage hasNextPage }"
                        + " }");

        JSONObject pagedConnection = getTagManagerResult(pagedResult).getJSONObject(TAGS);
        JSONArray pagedNodes = pagedConnection.getJSONArray(NODES);
        Assert.assertEquals(1, pagedNodes.length());
        Assert.assertEquals(BETA_TAG, pagedNodes.getJSONObject(0).getString("name"));

        JSONObject pagedPageInfo = pagedConnection.getJSONObject(PAGE_INFO);
        Assert.assertEquals(3, pagedPageInfo.getInt(TOTAL_COUNT));
        Assert.assertEquals(1, pagedPageInfo.getInt(NODES_COUNT));
        Assert.assertTrue(pagedPageInfo.getBoolean("hasPreviousPage"));
        Assert.assertTrue(pagedPageInfo.getBoolean("hasNextPage"));
    }

    @Test
    public void shouldReturnManagedTagsSortedByOccurrences() {
        JSONObject result = executeTagManagerQuery(
                " tags(sortBy: OCCURRENCES, sortOrder: DESC) {"
                        + "  nodes { name occurrences }"
                        + " }");

        JSONArray nodes = getTagManagerResult(result).getJSONObject(TAGS).getJSONArray(NODES);

        Assert.assertEquals(GAMMA_TAG, nodes.getJSONObject(0).getString("name"));
        Assert.assertEquals(3, nodes.getJSONObject(0).getLong(OCCURRENCES));
        Assert.assertEquals(BETA_TAG, nodes.getJSONObject(1).getString("name"));
        Assert.assertEquals(2, nodes.getJSONObject(1).getLong(OCCURRENCES));
        Assert.assertEquals(ALPHA_TAG, nodes.getJSONObject(2).getString("name"));
        Assert.assertEquals(1, nodes.getJSONObject(2).getLong(OCCURRENCES));
    }

    @Test
    public void shouldReturnTaggedContentScopedToTheRequestedSite() {
        JSONObject result = executeTagManagerQuery(
                " taggedContent(tag: \"" + GAMMA_TAG + "\", limit: 10, offset: 0) {"
                        + "  nodes { uuid path displayName primaryNodeType { name icon } }"
                        + "  pageInfo { totalCount nodesCount }"
                        + " }");

        JSONObject connection = getTagManagerResult(result).getJSONObject(TAGGED_CONTENT);
        JSONArray nodes = connection.getJSONArray(NODES);

        Assert.assertEquals(3, nodes.length());
        Assert.assertEquals(siteTagPath(ALPHA_NODE_NAME), nodes.getJSONObject(0).getString("path"));
        Assert.assertEquals(siteTagPath(BETA_NODE_NAME), nodes.getJSONObject(1).getString("path"));
        Assert.assertEquals(siteTagPath(GAMMA_NODE_NAME), nodes.getJSONObject(2).getString("path"));
        Assert.assertEquals(CONTENT_LIST_TYPE, nodes.getJSONObject(0).getJSONObject("primaryNodeType").getString("name"));
        Assert.assertTrue(nodes.getJSONObject(0).getJSONObject("primaryNodeType").has("icon"));

        JSONObject pageInfo = connection.getJSONObject(PAGE_INFO);
        Assert.assertEquals(3, pageInfo.getInt(TOTAL_COUNT));
        Assert.assertEquals(3, pageInfo.getInt(NODES_COUNT));
    }

    @Test
    public void shouldKeepTagSuggestQueryUnchanged() {
        JSONObject result = executeQuery("{ tag { suggest(prefix: \"ga\", limit: 10, startPath: \"" + sitePath(SITE_KEY) + "\", minCount: 1, offset: 0, sortByCount: true) {"
                + " name occurences"
                + "} } }");

        JSONArray suggestions = result.getJSONObject(DATA).getJSONObject("tag").getJSONArray("suggest");
        Assert.assertEquals(1, suggestions.length());
        Assert.assertEquals(GAMMA_TAG, suggestions.getJSONObject(0).getString("name"));
        Assert.assertEquals(3, suggestions.getJSONObject(0).getLong("occurences"));
    }

    @Test
    public void shouldHandleSpecialCharacterTagsForUsageRenameAndDelete() {
        String escapedTag = escapeGraphQLString(SPECIAL_TAG);

        JSONObject usagesResult = executeTagManagerQuery(
                " taggedContent(tag: \"" + escapedTag + "\", limit: 10, offset: 0) {"
                        + "  nodes { uuid path }"
                        + "  pageInfo { totalCount nodesCount }"
                        + " }");

        JSONObject usagesConnection = getTagManagerResult(usagesResult).getJSONObject(TAGGED_CONTENT);
        Assert.assertEquals(1, usagesConnection.getJSONObject(PAGE_INFO).getInt(TOTAL_COUNT));
        Assert.assertEquals(siteTagPath(GAMMA_NODE_NAME), usagesConnection.getJSONArray(NODES).getJSONObject(0).getString("path"));

        JSONObject renameResult = executeTagManagerMutation(
                " renameTag(tag: \"" + escapedTag + "\", newName: \"" + SPECIAL_RENAMED_TAG + "\") {"
                        + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                        + " }");

        JSONArray renameWorkspaceResults = getTagManagerResult(renameResult).getJSONObject("renameTag").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(1, renameWorkspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(1, renameWorkspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(GAMMA_NODE_NAME), GAMMA_TAG, SPECIAL_RENAMED_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(GAMMA_NODE_NAME), GAMMA_TAG, SPECIAL_RENAMED_TAG);

        JSONObject deleteResult = executeTagManagerMutation(
                " deleteTag(tag: \"" + SPECIAL_RENAMED_TAG + "\") {"
                        + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                        + " }");

        JSONArray deleteWorkspaceResults = getTagManagerResult(deleteResult).getJSONObject("deleteTag").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(1, deleteWorkspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(1, deleteWorkspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(GAMMA_NODE_NAME), GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(GAMMA_NODE_NAME), GAMMA_TAG);
    }

    @Test
    public void shouldRenameTagInEditAndLive() {
        JSONObject result = executeTagManagerMutation(
                " renameTag(tag: \"" + BETA_TAG + "\", newName: \"" + BETA_RENAMED_TAG + "\") {"
                + "  tag"
                + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                + " }");

        JSONArray workspaceResults = getTagManagerResult(result).getJSONObject("renameTag").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(2, workspaceResults.length());
        Assert.assertEquals(2, workspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(0, workspaceResults.getJSONObject(0).getJSONArray("failedNodes").length());
        Assert.assertEquals(2, workspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));
        Assert.assertEquals(0, workspaceResults.getJSONObject(1).getJSONArray("failedNodes").length());

        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_RENAMED_TAG, GAMMA_TAG);
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_RENAMED_TAG, GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_RENAMED_TAG, GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_RENAMED_TAG, GAMMA_TAG);
    }

    @Test
    public void shouldDeleteTagInEditAndLive() {
        JSONObject result = executeTagManagerMutation(
                " deleteTag(tag: \"" + GAMMA_TAG + "\") {"
                + "  tag"
                + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                + " }");

        JSONArray workspaceResults = getTagManagerResult(result).getJSONObject("deleteTag").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(3, workspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(3, workspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));

        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG);
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_TAG);
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(GAMMA_NODE_NAME));
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(GAMMA_NODE_NAME));
    }

    @Test
    public void shouldDeleteTagOnNodeInEditAndLive() {
        JSONObject result = executeTagManagerMutation(
                " deleteTagOnNode(tag: \"" + BETA_TAG + "\", nodeId: \"" + betaNodeId + "\") {"
                + "  tag"
                + "  nodeId"
                + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                + " }");

        JSONArray workspaceResults = getTagManagerResult(result).getJSONObject("deleteTagOnNode").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(1, workspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(1, workspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));

        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG, GAMMA_TAG);
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(BETA_NODE_NAME), GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG, GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(BETA_NODE_NAME), GAMMA_TAG);
    }

    @Test
    public void shouldRenameTagOnNodeInEditAndLive() {
        JSONObject result = executeTagManagerMutation(
                " renameTagOnNode(tag: \"" + BETA_TAG + "\", newName: \"" + BETA_LOCAL_TAG + "\", nodeId: \"" + betaNodeId + "\") {"
                + "  tag"
                + "  nodeId"
                + "  workspaceResults { workspace processedCount updatedNodes { path } failedNodes { path } }"
                + " }");

        JSONArray workspaceResults = getTagManagerResult(result).getJSONObject("renameTagOnNode").getJSONArray(WORKSPACE_RESULTS);
        Assert.assertEquals(1, workspaceResults.getJSONObject(0).getInt(PROCESSED_COUNT));
        Assert.assertEquals(1, workspaceResults.getJSONObject(1).getInt(PROCESSED_COUNT));

        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG, GAMMA_TAG);
        assertTags(Constants.EDIT_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_LOCAL_TAG, GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(ALPHA_NODE_NAME), ALPHA_TAG, BETA_TAG, GAMMA_TAG);
        assertTags(Constants.LIVE_WORKSPACE, siteTagPath(BETA_NODE_NAME), BETA_LOCAL_TAG, GAMMA_TAG);
    }

    private void createAndSeedSites() {
        try {
            TestHelper.createSite(SITE_KEY);
            TestHelper.createSite(OTHER_SITE_KEY);

            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, null);
            try {
                JCRNodeWrapper siteNode = session.getNode(sitePath(SITE_KEY));
                JCRNodeWrapper tagsFolder = siteNode.addNode(TAGS_FOLDER, CONTENT_LIST_TYPE);
                JCRNodeWrapper alphaNode = tagsFolder.addNode(ALPHA_NODE_NAME, CONTENT_LIST_TYPE);
                JCRNodeWrapper betaNode = tagsFolder.addNode(BETA_NODE_NAME, CONTENT_LIST_TYPE);
                JCRNodeWrapper gammaNode = tagsFolder.addNode(GAMMA_NODE_NAME, CONTENT_LIST_TYPE);

                taggingService.tag(alphaNode, Arrays.asList(ALPHA_TAG, BETA_TAG, GAMMA_TAG));
                taggingService.tag(betaNode, Arrays.asList(BETA_TAG, GAMMA_TAG));
                taggingService.tag(gammaNode, Arrays.asList(GAMMA_TAG, SPECIAL_TAG));

                betaNodeId = betaNode.getIdentifier();

                JCRNodeWrapper otherTagsFolder = session.getNode(sitePath(OTHER_SITE_KEY)).addNode(TAGS_FOLDER, CONTENT_LIST_TYPE);
                JCRNodeWrapper otherNode = otherTagsFolder.addNode(OUTSIDE_NODE_NAME, CONTENT_LIST_TYPE);
                taggingService.tag(otherNode, Collections.singletonList(GAMMA_TAG));

                session.save();
                JCRPublicationService.getInstance().publishByMainId(siteNode.getIdentifier());
                JCRPublicationService.getInstance().publishByMainId(session.getNode(sitePath(OTHER_SITE_KEY)).getIdentifier());
            } finally {
                session.logout();
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to create and seed tag manager test sites", e);
        }
    }

    private void removeSites() {
        removeSite(Constants.EDIT_WORKSPACE, SITE_KEY);
        removeSite(Constants.EDIT_WORKSPACE, OTHER_SITE_KEY);
        removeSite(Constants.LIVE_WORKSPACE, SITE_KEY);
        removeSite(Constants.LIVE_WORKSPACE, OTHER_SITE_KEY);
    }

    private void removeSite(String workspace, String siteKey) {
        JCRSessionWrapper session;
        try {
            session = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, Locale.ENGLISH, null);
        } catch (RepositoryException e) {
            throw new AssertionError("Failed to open system session for workspace " + workspace, e);
        }

        try {
            if (session.nodeExists(sitePath(siteKey))) {
                session.getNode(sitePath(siteKey)).remove();
                session.save();
            }
        } catch (RepositoryException e) {
            throw new AssertionError("Failed to remove site " + siteKey + " in workspace " + workspace, e);
        } finally {
            session.logout();
        }
    }

    private void assertTags(String workspace, String path, String... expectedTags) {
        JCRSessionWrapper session;
        try {
            session = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, Locale.ENGLISH, null);
        } catch (RepositoryException e) {
            throw new AssertionError("Failed to open system session for workspace " + workspace, e);
        }

        try {
            Assert.assertEquals(Arrays.asList(expectedTags), getTags(session.getNode(path)));
        } catch (RepositoryException e) {
            throw new AssertionError("Failed to assert tags for " + path + " in workspace " + workspace, e);
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

    private JSONObject executeTagManagerQuery(String body) {
        return executeQuery(QUERY_PREFIX + SITE_KEY + QUERY_SUFFIX + body + QUERY_END);
    }

    private JSONObject executeTagManagerMutation(String body) {
        return executeQuery(MUTATION_PREFIX + SITE_KEY + QUERY_SUFFIX + body + QUERY_END);
    }

    private JSONObject getTagManagerResult(JSONObject result) {
        return result.getJSONObject(DATA).getJSONObject(ADMIN).getJSONObject(JAHIA).getJSONObject(TAG_MANAGER);
    }

    private String sitePath(String siteKey) {
        return SITE_ROOT + siteKey;
    }

    private String siteTagPath(String nodeName) {
        return sitePath(SITE_KEY) + TAGS_ROOT + nodeName;
    }
}
