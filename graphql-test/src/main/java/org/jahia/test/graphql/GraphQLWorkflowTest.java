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
