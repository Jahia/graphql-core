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

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.RepositoryException;

public class GraphQLi18nTest extends GraphQLTestSupport {

    @BeforeClass
    public static void oneTimeSetup() throws Exception {

        GraphQLTestSupport.init();

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH, session -> {
            JCRNodeWrapper node = session.getNode("/").addNode("testList", "jnt:contentList");
            JCRNodeWrapper subNode = node.addNode("testSubList", "jnt:contentList");
            subNode.addNode("testSubSubList", "jnt:contentList");
            session.save();
            return null;
        });
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        GraphQLTestSupport.removeTestNodes();
    }

    private static void updateTitle(Locale locale) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, locale, session -> {
            JCRNodeWrapper node = session.getNode("/testList");
            node.setProperty("jcr:title", locale.toString() + System.currentTimeMillis());
            session.save();
            return null;
        });
    }

    private List<String> getLanguagesToTranslate(List<String> translatedLanguages, List<String> languagesToBeChecked) throws JSONException {
        JSONObject result = executeQuery("{"
                + "    jcr {"
                + "    nodeByPath(path: \"/testList\") {"
                + "        languagesToTranslate(languagesTranslated: [\"" + String.join("\",\"", translatedLanguages) + "\"], languagesToCheck: [\"" + String.join("\",\"", languagesToBeChecked) + "\"])"
                + "    }"
                + "    }"
                + "}");
        JSONArray languages = result.getJSONObject("data").getJSONObject("jcr").getJSONObject("nodeByPath").getJSONArray("languagesToTranslate");

        List<String> foundLanguages = new ArrayList<>();
        for (int i = 0; i < languages.length(); i++) {
            foundLanguages.add(languages.getString(i));
        }
        return foundLanguages;
    }

    @Test
    public void shouldRetrieveRequiredTranslations() throws Exception {
        updateTitle(Locale.ENGLISH);
        sleep(2000);
        updateTitle(Locale.FRENCH);
        List<String> toBeTranslated = getLanguagesToTranslate(Collections.singletonList("fr"), Collections.singletonList("en"));

        Assert.assertEquals(1, toBeTranslated.size());
        Assert.assertTrue(toBeTranslated.contains("en"));

        sleep(2000);
        updateTitle(Locale.ENGLISH);

        toBeTranslated = getLanguagesToTranslate(Collections.singletonList("fr"), Collections.singletonList("en"));
        Assert.assertEquals(0, toBeTranslated.size());
        toBeTranslated = getLanguagesToTranslate(Collections.singletonList("en"), Collections.singletonList("fr"));
        Assert.assertEquals(1, toBeTranslated.size());
        Assert.assertTrue(toBeTranslated.contains("fr"));
        toBeTranslated = getLanguagesToTranslate(Collections.singletonList("en"), Arrays.asList("fr", "de"));
        Assert.assertEquals(2, toBeTranslated.size());
        Assert.assertTrue(toBeTranslated.contains("fr"));
        Assert.assertTrue(toBeTranslated.contains("de"));

        sleep(2000);
        updateTitle(Locale.FRENCH);
        sleep(2000);
        updateTitle(Locale.GERMAN);

        toBeTranslated = getLanguagesToTranslate(Arrays.asList("de", "en"), Collections.singletonList("fr"));
        Assert.assertEquals(1, toBeTranslated.size());
        Assert.assertTrue(toBeTranslated.contains("fr"));

        toBeTranslated = getLanguagesToTranslate(Arrays.asList("fr", "en"), Collections.singletonList("de"));
        Assert.assertEquals(0, toBeTranslated.size());

        toBeTranslated = getLanguagesToTranslate(Arrays.asList("fr", "de"), Collections.singletonList("en"));
        Assert.assertEquals(1, toBeTranslated.size());
        Assert.assertTrue(toBeTranslated.contains("en"));
    }
}

