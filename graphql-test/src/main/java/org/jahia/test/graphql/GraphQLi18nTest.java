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

