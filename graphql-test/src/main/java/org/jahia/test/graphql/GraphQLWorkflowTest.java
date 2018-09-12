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

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.workflow.WorkflowService;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

/**
 * Short description of the class
 *
 * @author ybenchadi
 */
public class GraphQLWorkflowTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            session.save();
            WorkflowService.getInstance().startProcess(Collections.singletonList(node.getIdentifier()), session,
                    "1-step-publication", "jBPM", new HashMap<>(), null);
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    @Test
    public void shouldRetrieveWorkflowTasksForUser() throws Exception {
        JSONObject result = executeQuery("{"
                + "jcr {"
                + "     workflowTasksForUser(language: \"en\")"
                + "     }"
                + "}");

        int tasks = result.getJSONObject("data").getJSONObject("jcr").getInt("workflowTasksForUser");
        Assert.assertEquals(1, tasks);
    }



}
