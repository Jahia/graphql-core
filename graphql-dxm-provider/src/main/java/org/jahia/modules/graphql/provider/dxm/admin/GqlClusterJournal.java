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
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.services.events.JournalEventReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


@GraphQLName("Journal")
@GraphQLDescription("Details about the Jahia cluster journal")
public class GqlClusterJournal {

    public static final Logger logger = LoggerFactory.getLogger(GqlClusterJournal.class);

    @Inject
    @GraphQLOsgiService
    private JournalEventReader journalEventReader;

    @GraphQLField
    @GraphQLDescription("The latest revision of the journal on the cluster")
    public Long getGlobalRevision() {
        return journalEventReader.getGlobalRevision();
    }

    @GraphQLField
    @GraphQLDescription("The latest revision of the journal on the current node")
    public GqlClusterJournalLocalRevision getLocalRevision() {
        return new GqlClusterJournalLocalRevision(journalEventReader.getNodeId(), journalEventReader.getLocalRevision());
    }

    @GraphQLField
    @GraphQLDescription("The latest revisions of the journal for all cluster nodes")
    public List<GqlClusterJournalLocalRevision> getRevisions() {
        return journalEventReader.getRevisions().entrySet().stream()
                .map(e -> new GqlClusterJournalLocalRevision(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("isClusterSync")
    @GraphQLDescription("Is the journal in sync across all nodes of a cluster")
    public Boolean isClusterSync() {
        return journalEventReader.isClusterSync();
    }
}
