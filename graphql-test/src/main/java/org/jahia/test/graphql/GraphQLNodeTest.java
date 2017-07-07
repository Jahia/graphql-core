/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.graphql;

import graphql.servlet.OsgiGraphQLServlet;
import org.jahia.osgi.BundleUtils;
import org.jahia.test.JahiaTestCase;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.Servlet;

import static org.junit.Assert.*;

/**¡
 * Unit test for remote publishing
 *
 */
public class GraphQLNodeTest extends JahiaTestCase {
    private static OsgiGraphQLServlet servlet;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
    }

    private JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }

    @Test
    public void testGetNode() throws Exception {
        JSONObject result = executeQuery("{ nodeByPath(path: \"/sites\") { name path } }");
        JSONObject nodeByPath = result.getJSONObject("data").getJSONObject("nodeByPath");
        assertEquals("/sites", nodeByPath.getString("path"));
        assertEquals("sites", nodeByPath.getString("name"));
    }


}