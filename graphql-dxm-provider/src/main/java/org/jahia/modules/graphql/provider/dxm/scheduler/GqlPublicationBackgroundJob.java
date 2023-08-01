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
package org.jahia.modules.graphql.provider.dxm.scheduler;


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.*;
import org.quartz.JobDetail;

import javax.jcr.RepositoryException;
import java.util.*;

@GraphQLDescription("Publication background job")
public class GqlPublicationBackgroundJob extends GqlBackgroundJob {

    public GqlPublicationBackgroundJob() {
    }

    public GqlPublicationBackgroundJob(JobDetail jobDetail, GqlBackgroundJobState state) {
        super(jobDetail, state);
    }

    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("Publication language")
    public String getLanguage() {
        JCRSessionWrapper session;

        try {
            JCRNodeWrapper translationNode = null;
            session = JCRSessionFactory.getInstance().getCurrentUserSession();
            Collection<String> uuids = (Collection<String>) this.jobDetail.getJobDataMap().get(PublicationJob.PUBLICATION_UUIDS);
            Iterator<String> it = uuids.iterator();
            while (it.hasNext() && translationNode == null) {
                String uuid = it.next();
                translationNode = session.getNodeByIdentifier(uuid);
                if (!translationNode.isNodeType("jnt:translation")) {
                    translationNode = null;
                }
            }

            if (translationNode != null) {
                return translationNode.getLanguage();
            }
        } catch (RuntimeException | RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }

        return null;
    }
}
