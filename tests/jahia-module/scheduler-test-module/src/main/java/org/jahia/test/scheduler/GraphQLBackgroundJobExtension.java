/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.scheduler;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminQuery;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.osgi.service.component.annotations.Component;
import org.quartz.SchedulerException;

@Component(service = {DXGraphQLExtensionsProvider.class}, immediate = true)
@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("A query extension that allows to create and start a background job")
public class GraphQLBackgroundJobExtension implements DXGraphQLExtensionsProvider {

    @GraphQLField
    @GraphQLDescription("Create and start a background job")
    public static boolean createAndStartJob(@GraphQLName("jobName") @GraphQLDescription("The name of the test job") @GraphQLNonNull String jobName) throws SchedulerException {
        SchedulerService schedulerService = BundleUtils.getOsgiService(SchedulerService.class, null);
        schedulerService.scheduleJobNow(TestJob.createTestJobDetail(jobName));
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Stop and delete a background job")
    public static boolean stopAndDeleteJob(@GraphQLName("jobName") @GraphQLDescription("The name of the test job") @GraphQLNonNull String jobName) throws SchedulerException {
        SchedulerService schedulerService = BundleUtils.getOsgiService(SchedulerService.class, null);
        return schedulerService.getAllJobs().stream().filter(jobDetail -> jobName.equals(jobDetail.getName())).findFirst()
                .map(jobDetail -> {
                    try {
                        return schedulerService.getScheduler().deleteJob(jobDetail.getName(), BackgroundJob.getGroupName(TestJob.class));
                    } catch (SchedulerException e) {
                        return null;
                    }
                }).orElseThrow(() -> new SchedulerException(String.format("Job '%s' not found", jobName)));
    }
}
