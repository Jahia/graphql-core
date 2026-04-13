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
package org.jahia.modules.graphql.provider.dxm.service.tags.service;

import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.filter.cache.ModuleCacheProvider;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.Locale;

abstract class TagManagerServiceSupport {

    protected static void flushNodeCaches(ModuleCacheProvider moduleCacheProvider, String path) {
        moduleCacheProvider.invalidate(path, true);
        moduleCacheProvider.flushRegexpDependenciesOfPath(path, true);
    }

    protected JCRSessionWrapper getCurrentUserEditSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
    }

    protected JCRNodeWrapper getAuthorizedSiteNode(String siteKey, JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper siteNode = session.getNode("/sites/" + siteKey);
        if (!siteNode.hasPermission("tagManager")) {
            throw new DataFetchingException("Permission denied");
        }

        return siteNode;
    }

    protected void validateNodeBelongsToSite(JCRSessionWrapper session, String nodeId, String sitePath) throws RepositoryException {
        JCRNodeWrapper node = session.getNodeByIdentifier(nodeId);
        String nodePath = node.getPath();
        if (!nodePath.equals(sitePath) && !nodePath.startsWith(sitePath + "/")) {
            throw new GqlJcrWrongInputException("Node does not belong to the requested site");
        }
    }

    protected JCRSessionWrapper getSystemSession(String workspace, Locale locale, Locale fallbackLocale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, locale, fallbackLocale);
    }

    protected NodeIterator findTaggedNodes(String sitePath, String tag, JCRSessionWrapper session) throws RepositoryException {
        String query = "SELECT * FROM [jmix:tagged] AS result WHERE ISDESCENDANTNODE(result, '" +
                JCRContentUtils.sqlEncode(sitePath) + "') AND (result.[j:tagList] = $tag)";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query jcrQuery = queryManager.createQuery(query, Query.JCR_SQL2);
        Value tagValue = session.getValueFactory().createValue(tag);
        jcrQuery.bindValue("tag", tagValue);
        return jcrQuery.execute().getNodes();
    }
}
