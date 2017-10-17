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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.Servlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

public class GraphQLAbstractTest extends JahiaTestCase {

    private static OsgiGraphQLServlet servlet;

    static String nodeUuid;
    static String nodeTitleFr = "text FR";
    static String nodeTitleEn = "text EN";
    static String subNodeUuid1;
    static String subNodeUuid2;
    static String subNodeUuid3;
    static String subNodeUuid4;
    static String subnodeTitleFr1 = "text FR - subList1";
    static String subnodeTitleEn1 = "text EN - subList1";
    static String subnodeTitleFr2 = "text FR - subList2";
    static String subnodeTitleEn2 = "text EN - subList2";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        servlet = (OsgiGraphQLServlet) BundleUtils.getOsgiService(Servlet.class, "(component.name=graphql.servlet.OsgiGraphQLServlet)");

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                session -> {

                    if (session.getNode("/").hasNode("testList")) {
                        session.getNode("/testList").remove();
                        session.save();
                    }

                    JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
                    node.addMixin("jmix:liveProperties");
                    node.setProperty("jcr:title", nodeTitleEn);
                    node.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                    nodeUuid = node.getIdentifier();

                    JCRNodeWrapper subNode1 = node.addNode("testSubList1", "jnt:contentList");
                    subNode1.addMixin("jmix:liveProperties");
                    subNode1.setProperty("jcr:title", subnodeTitleEn1);
                    subNode1.setProperty("j:liveProperties", new String[] {"liveProperty1", "liveProperty2"});
                    subNodeUuid1 = subNode1.getIdentifier();

                    JCRNodeWrapper subNode2 = node.addNode("testSubList2", "jnt:contentList");
                    subNode2.setProperty("jcr:title", subnodeTitleEn2);
                    subNodeUuid2 = subNode2.getIdentifier();

                    JCRNodeWrapper subNode3 = node.addNode("testSubList3", "jnt:contentList");
                    subNodeUuid3 = subNode3.getIdentifier();

                    JCRNodeWrapper subNode4 = node.addNode("testSubList4", "jnt:contentList");
                    subNodeUuid4 = subNode4.getIdentifier();
                    subNode4.addNode("testSubList4_1", "jnt:contentList");
                    subNode4.addNode("testSubList4_2", "jnt:contentList");
                    subNode4.addNode("testSubList4_3", "jnt:contentList");

                    JCRNodeWrapper ref1 = node.addNode("reference1", "jnt:contentReference");
                    ref1.setProperty("j:node", subNode1);
                    JCRNodeWrapper ref2 = node.addNode("reference2", "jnt:contentReference");
                    ref2.setProperty("j:node", subNode1);
                    JCRNodeWrapper ref3 = node.addNode("reference3", "jnt:contentReference");
                    ref3.setProperty("j:node", subNode1);

                    session.save();
                    return null;
                });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.FRENCH,
                session -> {
                    JCRNodeWrapper node = session.getNode("/testList");
                    node.setProperty("jcr:title", nodeTitleFr);
                    node.getNode("testSubList1").setProperty("jcr:title", subnodeTitleFr1);
                    node.getNode("testSubList2").setProperty("jcr:title", subnodeTitleFr2);
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

    protected void testQuery(String query, String language, long expectedNumber) throws Exception {
        JSONObject result = executeQuery("{"
                + "    nodesByQuery(query: \"" + query + "\", queryLanguage: " + language + ") {"
                + "        edges {"
                + "            node {"
                + "                name"
                + "                path"
                + "            }"
                + "		  }"
                + "    }"
                + "}");

        JSONArray nodes = result.getJSONObject("data").getJSONObject("nodesByQuery").getJSONArray("edges");
        Assert.assertEquals(expectedNumber, nodes.length());
    }

    static Map<String, JSONObject> toItemByKeyMap(String key, JSONArray items) throws JSONException {
        HashMap<String, JSONObject> itemByName = new HashMap<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            itemByName.put(item.getString(key), item);
        }
        return itemByName;
    }

    static void validateSingleValuedProperty(JSONObject property, String expectedName, GqlJcrPropertyType expectedType, String expectedParentNodePath, boolean expectedInternationalized, Object expectedLanguage, String expectedValue) throws JSONException {
        Assert.assertEquals(expectedName, property.getString("name"));
        Assert.assertEquals(expectedType.name(), property.getString("type"));
        Assert.assertEquals(expectedParentNodePath, property.getJSONObject("parentNode").getString("path"));
        validateSingleValuedProperty(property, expectedInternationalized, expectedLanguage, expectedValue);
    }

    static void validateNode(JSONObject node, String expectedName) throws JSONException {
        Assert.assertEquals(expectedName, node.getString("name"));
    }

    static void validateNode(JSONObject node, String expectedUuid, String expectedName, String expectedPath, String expectedParentNodePath) throws JSONException {
        validateNode(node, expectedName);
        Assert.assertEquals(expectedUuid, node.getString("uuid"));
        Assert.assertEquals(expectedPath, node.getString("path"));
        Assert.assertEquals(expectedParentNodePath, node.getJSONObject("parent").getString("path"));
    }

    JSONObject executeQuery(String query) throws JSONException {
        return new JSONObject(servlet.executeQuery(query));
    }

    static void validateSingleValuedProperty(JSONObject property, boolean expectedInternationalized, Object expectedLanguage, String expectedValue)
            throws JSONException {
        Assert.assertEquals(expectedInternationalized, property.getBoolean("internationalized"));
        Assert.assertEquals(expectedLanguage, property.get("language"));
        Assert.assertEquals(expectedValue, property.getString("value"));
        Assert.assertEquals(JSONObject.NULL, property.get("values"));
    }

}