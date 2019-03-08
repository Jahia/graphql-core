/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
