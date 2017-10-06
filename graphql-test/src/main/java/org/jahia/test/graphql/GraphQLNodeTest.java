/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 * http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 * Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
 *
 * This file is part of a Jahia's Enterprise Distribution.
 *
 * Jahia's Enterprise Distributions must be used in accordance with the terms
 * contained in the Jahia Solutions Group Terms & Conditions as well as
 * the Jahia Sustainable Enterprise License (JSEL).
 *
 * For questions regarding licensing, support, production usage...
 * please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.graphql;

import graphql.servlet.OsgiGraphQLServlet;

import org.jahia.api.Constants;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.Servlet;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * ¡
 * Unit test for remote publishing
 */
public class GraphQLNodeTest extends JahiaTestCase {
    private static OsgiGraphQLServlet servlet;

    private static String testedNodeUUID = null;
    private static String testedNodeTitleFR = "text FR";
    private static String testedNodeTitleEN = "text EN";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                session -> {
                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }
                    JCRNodeWrapper testedNode = session.getNode("/").addNode("testList", "jnt:contentList");
                    testedNode.setProperty("jcr:title", testedNodeTitleEN);
                    testedNodeUUID = testedNode.getIdentifier();
                    session.save();
                    return null;
                });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    JCRNodeWrapper testedNode = session.getNode("/testList");
                    testedNode.setProperty("jcr:title", testedNodeTitleFR);
                    session.save();
                    return null;
                }
        );
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }
                    return null;
                });
    }

    @Test
    public void testGetNode() throws Exception {

        JSONObject result = executeQuery("{ nodeByPath(path: \"/testList\") { name path uuid displayName \t titleen:property(name: \"jcr:title\", " +
                "language:\"en\") {\n" +
                "        value\n" +
                "      } \n" +
                "    \t titlefr:property(name: \"jcr:title\", language:\"fr\") {\n" +
                "        value\n" +
                "      } \n" +
                "    } }");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");

        assertEquals("/testList", nodeByPath.getString("path"));
        assertEquals("testList", nodeByPath.getString("name"));
        assertEquals(testedNodeUUID, nodeByPath.getString("uuid"));
        assertEquals("testList", nodeByPath.getString("displayName"));
        assertEquals(testedNodeTitleFR, nodeByPath.getJSONObject("titlefr").getString("value"));
        assertEquals(testedNodeTitleEN, nodeByPath.getJSONObject("titleen").getString("value"));

    }

    private JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }

}