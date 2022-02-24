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
package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.*;
import org.apache.commons.lang.BooleanUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.*;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Publication mutation extensions for JCR node.
 */
@GraphQLTypeExtension(GqlJcrNodeMutation.class)
public class PublicationJCRNodeMutationExtension extends PublicationJCRExtensionSupport {

    private GqlJcrNodeMutation nodeMutation;

    /**
     * Create a publication mutation extension instance.
     *
     * @param nodeMutation JCR node mutation to apply the extension to
     * @throws GqlJcrWrongInputException In case the parameter represents a node from LIVE rather than EDIT workspace
     */
    public PublicationJCRNodeMutationExtension(GqlJcrNodeMutation nodeMutation) throws GqlJcrWrongInputException {
        validateNodeWorkspace(nodeMutation.getNode());
        this.nodeMutation = nodeMutation;
    }

    /**
     * Publish the node in certain languages.
     *
     * @param languages Languages to publish the node in
     * @param publishSubNodes Publish all sub and related nodes. Default is true.
     * @param publishAllSubTree Publish all sub tree including sub pages. Default is false.
     * @return Always true
     */
    @GraphQLField
    @GraphQLDescription("Publish the node in certain languages")
    public boolean publish(@GraphQLName("languages") @GraphQLDescription("Languages to publish the node in") Collection<String> languages,
                           @GraphQLName("publishSubNodes") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) @GraphQLDescription("Publish all sub and related nodes. Default is true.") Boolean publishSubNodes,
            @GraphQLName("includeSubTree") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) @GraphQLDescription("Publish all sub tree including sub pages. Default is false.") Boolean publishAllSubTree) {

        ComplexPublicationService publicationService = BundleUtils.getOsgiService(ComplexPublicationService.class, null);
        SchedulerService schedulerService = BundleUtils.getOsgiService(SchedulerService.class, null);

        String uuid;
        String path;
        JCRSessionWrapper session;
        try {
            JCRNodeWrapper nodeToPublish = nodeMutation.getNode().getNode();
            uuid = nodeToPublish.getIdentifier();
            path = nodeToPublish.getPath();
            session = JCRSessionFactory.getInstance().getCurrentUserSession();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }

        if (BooleanUtils.isFalse(publishSubNodes)) {
            JobDetail jobDetail = BackgroundJob.createJahiaJob("Publication", PublicationJob.class);
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.put(PublicationJob.PUBLICATION_UUIDS, Collections.singletonList(uuid));
            jobDataMap.put(PublicationJob.PUBLICATION_PATHS, Collections.singletonList(path));
            jobDataMap.put(PublicationJob.SOURCE, Constants.EDIT_WORKSPACE);
            jobDataMap.put(PublicationJob.DESTINATION, Constants.LIVE_WORKSPACE);
            jobDataMap.put(PublicationJob.CHECK_PERMISSIONS, true);
            try {
                schedulerService.scheduleJobNow(jobDetail);
            } catch (SchedulerException e) {
                throw new JahiaRuntimeException(e);
            }
        } else {
            publicationService.publish(Collections.singleton(uuid), languages, session, publishAllSubTree);
        }

        return true;
    }

    /**
     * Unpublish the node in certain languages.
     *
     * @param languages Languages to unpublish the node in
     * @return Always true
     */
    @GraphQLField
    @GraphQLDescription("Unpublish the node in certain languages")
    public boolean unpublish(@GraphQLName("languages") @GraphQLDescription("Languages to publish the node in") Collection<String> languages) {

        ComplexPublicationService publicationService = BundleUtils.getOsgiService(ComplexPublicationService.class, null);
        JCRPublicationService jcrPublicationService = BundleUtils.getOsgiService(JCRPublicationService.class, null);

        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            Collection<ComplexPublicationService.FullPublicationInfo> fullPublicationInfo = publicationService.
                    getFullUnpublicationInfos(Collections.singletonList(nodeMutation.getNode().getNode().getIdentifier()), languages, false, session);
            List<String> uuidsToUnpublish = getAllUuids(fullPublicationInfo, true);
            jcrPublicationService.unpublish(uuidsToUnpublish, true);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }

        return true;
    }

    // logic copy/pastes from GWT PublicationWorkflow.getAllUuids()
    private static List<String> getAllUuids(Collection<ComplexPublicationService.FullPublicationInfo> fullPublicationInfo, boolean onlyAllowedToPublishWithoutWorkflow) {
        List<String> l = new ArrayList<String>();
        for (ComplexPublicationService.FullPublicationInfo info : fullPublicationInfo) {
            if (info.getPublicationStatus() != PublicationInfo.DELETED && (!onlyAllowedToPublishWithoutWorkflow || info.isAllowedToPublishWithoutWorkflow())) {
                if (info.getNodeIdentifier() != null) {
                    l.add(info.getNodeIdentifier());
                }
                if (info.getTranslationNodeIdentifier() != null) {
                    l.add(info.getTranslationNodeIdentifier());
                }
                if (info.getDeletedTranslationNodeIdentifiers() != null) {
                    l.addAll(info.getDeletedTranslationNodeIdentifiers());
                }
            }
        }
        return l;
    }

}
