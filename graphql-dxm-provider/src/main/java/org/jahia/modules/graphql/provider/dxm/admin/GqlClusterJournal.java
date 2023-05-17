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
package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.jahia.services.SpringContextSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@GraphQLName("journal")
@GraphQLDescription("Details about the Jahia cluster journal")
public class GqlClusterJournal {

    public static final Logger logger = LoggerFactory.getLogger(GqlClusterJournal.class);

    // TODO (TECH-1284): Jahia core should provide a service to be able to get the necessary data instead of using hibernate and direct database queries
    private final SessionFactory hibernateSessionFactory = (SessionFactory) SpringContextSingleton.getBean("sessionFactory");

    @GraphQLField
    @GraphQLName("globalRevision")
    @GraphQLDescription("Get global revision")
    public Long getGlobalRevision() {
        try (Session session = hibernateSessionFactory.openSession()) {
            return queryGlobalRevision(session);
        }
    }

    @GraphQLField
    @GraphQLName("localRevision")
    @GraphQLDescription("Get local node revision")
    public Long getLocalRevision() {
        String currentNodeServerId = System.getProperty("cluster.node.serverId");
        if (!StringUtils.isEmpty(currentNodeServerId)) {
            try (Session session = hibernateSessionFactory.openSession()) {
                return queryLocalRevision(session, currentNodeServerId);
            }
        } else {
            logger.warn("Unable to query localRevision, cluster.node.serverId system property not found");
            return null;
        }
    }

    @GraphQLField
    @GraphQLName("isClusterSync")
    @GraphQLDescription("Check if all nodes locales revisions are equals to the global revision")
    public Boolean isClusterSync() {
        try (Session session = hibernateSessionFactory.openSession()) {
            Long globalRevision = queryGlobalRevision(session);
            if (globalRevision == null) {
                throw new IllegalStateException("Unable to check if cluster is sync, globalRevision not found");
            }

            List<Long> revisions = new ArrayList<>(queryAllLocalRevisions(session));
            revisions.add(globalRevision);

            return revisions.stream().distinct().count() <= 1;
        }
    }

    private Long queryLocalRevision(Session session, String journalId) {
        NativeQuery<?> query = session.createSQLQuery("SELECT REVISION_ID FROM JR_J_LOCAL_REVISIONS WHERE JOURNAL_ID = :journalId");
        query.setParameter("journalId", journalId);
        Object result = query.uniqueResult();
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return null;
    }

    private List<Long> queryAllLocalRevisions(Session session) {
        NativeQuery<?> query = session.createSQLQuery("SELECT REVISION_ID FROM JR_J_LOCAL_REVISIONS");
        List<?> results = query.getResultList();
        if (results != null) {
            return results.stream()
                    .filter(obj -> obj instanceof Number)
                    .map(obj -> ((Number) obj).longValue())
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Long queryGlobalRevision(Session session) {
        NativeQuery<?> query = session.createSQLQuery("SELECT REVISION_ID FROM JR_J_GLOBAL_REVISION");
        Object result = query.uniqueResult();
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return null;
    }
}
