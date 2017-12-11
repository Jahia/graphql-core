/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
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

    protected static void validateError(JSONObject result, String errorMessage) throws JSONException {
        JSONArray errors = result.getJSONArray("errors");
        Assert.assertEquals(1, errors.length());
        Assert.assertEquals(errorMessage, errors.getJSONObject(0).getString("message"));
    }
}