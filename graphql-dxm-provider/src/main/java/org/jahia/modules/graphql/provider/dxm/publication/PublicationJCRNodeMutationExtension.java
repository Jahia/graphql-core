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
import java.util.Collection;
import java.util.Collections;

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
     * @return Always true
     */
    @GraphQLField
    @GraphQLDescription("Publish the node in certain languages")
    public boolean publish(@GraphQLName("languages") @GraphQLDescription("Languages to publish the node in") Collection<String> languages,
                           @GraphQLName("publishSubNodes") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) @GraphQLDescription("Publish all sub and related nodes. Default is true.") Boolean publishSubNodes) {

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
            publicationService.publish(Collections.singleton(uuid), languages, session);
        }

        return true;
    }
}
