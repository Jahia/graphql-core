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
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
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

import java.util.Collections;
import java.util.HashMap;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.workflow.WorkflowService;
import org.jahia.test.TestHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to test Workflows graphQL API
 *
 * @author yousria
 */
public class GraphQLWorkflowTest extends GraphQLTestSupport {

    public static String TASK_ID;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        GraphQLTestSupport.init();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
        if (TASK_ID != null) {
            WorkflowService.getInstance().abortProcess(TASK_ID, "jBPM");
        }
    }

    @Test
    public void shouldRetrieveWorkflowTasksForUser() throws Exception {
        int initialTaskCount = getActiveTasksForUser();

        // start new task
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(getUser(), Constants.EDIT_WORKSPACE, null, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            session.save();
            TASK_ID =   WorkflowService.getInstance().startProcess(Collections.singletonList(node.getIdentifier()), session,
                    "1-step-publication", "jBPM", new HashMap<>(), null);
            session.save();
            WorkflowService.getInstance().assignTask(TASK_ID, "jBPM", getUser());
            session.save();
            return null;
        });
        TestHelper.triggerScheduledJobsAndWait();
        Assert.assertEquals(initialTaskCount + 1, getActiveTasksForUser());
    }

    private int getActiveTasksForUser() throws JSONException {
        JSONObject result = executeQuery("{"
                + "jcr {"
                + "     activeWorkflowTaskCountForUser"
                + "     }"
                + "}");

        return result.getJSONObject("data").getJSONObject("jcr").getInt("activeWorkflowTaskCountForUser");
    }
}
