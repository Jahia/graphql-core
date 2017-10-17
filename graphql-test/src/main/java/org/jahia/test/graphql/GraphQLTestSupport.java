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
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

public class GraphQLTestSupport extends JahiaTestCase {

    private static OsgiGraphQLServlet servlet;

    protected static void init() {
        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");
    }

    protected static void removeTestNodes() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH, session -> {
            session.getNode("/testList").remove();
            session.save();
            return null;
        });
    }

    protected static JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }

    protected static Map<String, JSONObject> toItemByKeyMap(String key, JSONArray items) throws JSONException {
        HashMap<String, JSONObject> itemByName = new HashMap<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            itemByName.put(item.getString(key), item);
        }
        return itemByName;
    }

    protected static void validateNode(JSONObject node, String expectedName) throws JSONException {
        Assert.assertEquals(expectedName, node.getString("name"));
    }

    protected static void validateNode(JSONObject node, String expectedUuid, String expectedName, String expectedPath, String expectedParentNodePath) throws JSONException {
        validateNode(node, expectedName);
        Assert.assertEquals(expectedUuid, node.getString("uuid"));
        Assert.assertEquals(expectedPath, node.getString("path"));
        Assert.assertEquals(expectedParentNodePath, node.getJSONObject("parent").getString("path"));
    }
}