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
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.osgi.service.component.annotations.Component;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

@Component(service = {DXGraphQLExtensionsProvider.class}, immediate = true)
@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("A query extension that allows to create and start a background job")
/*
 * This class allows the test in graphqlScheduler.cy.ts to create, start, stop and delete a background job.
 * NB: it's not possible to use a Groovy script as a job class defined in a Groovy is not available to the Quartz scheduler.
 */
public class GraphQLBackgroundJobExtension implements DXGraphQLExtensionsProvider {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLBackgroundJobExtension.class);

    @GraphQLField
    @GraphQLDescription("Create and start a background job for graphqlScheduler.cy.ts")
    public static boolean createAndStartJobForGraphQLSchedulerCypressTest(@GraphQLName("jobName") @GraphQLDescription("The name of the test job") @GraphQLNonNull String jobName) throws SchedulerException, NoSuchFieldException, IllegalAccessException {
        SchedulerService schedulerService = ServicesRegistry.getInstance().getSchedulerService();
        if (waitForJobSubscriptionToBeCreated(schedulerService)) {
            schedulerService.scheduleJobNow(TestJob.createTestJobDetail(jobName));
            return true;
        }
        return false;
    }

    // a bit of a hack to wait for the job listener to be created (done via a job subscription over websocket)
    // use double introspection to access private fields (SchedulerService#jahiaGlobalRamJobListener and JahiaJobListener#jobListeners)
    // this prevents the need to use sleeps in the test which could lead to flakiness
    private static boolean waitForJobSubscriptionToBeCreated(SchedulerService schedulerService) throws IllegalAccessException, NoSuchFieldException {
        Field jahiaGlobalRamJobListenerField = schedulerService.getClass().getDeclaredField("jahiaGlobalRamJobListener");
        jahiaGlobalRamJobListenerField.setAccessible(true);
        Object jahiaJobListener = jahiaGlobalRamJobListenerField.get(schedulerService);
        Field jobListenersField = jahiaJobListener.getClass().getDeclaredField("jobListeners");
        jobListenersField.setAccessible(true);
        Object jobListeners = jobListenersField.get(jahiaJobListener);
        Map<?, ?> jobListenersMap = (Map<?, ?>) jobListeners;
        long startTime = System.currentTimeMillis();
        while (jobListenersMap.isEmpty()) {
            if (System.currentTimeMillis() - startTime > 10000) { // 10 seconds timeout
                logger.error("Timeout waiting for the job listener to be created");
                return false;
            }
            try {
                // wait for the job listener to be added
                Thread.sleep(200);
                logger.info("Waiting for job listener to be created...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Job listener created successfully");
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Stop and delete a background job for graphqlScheduler.cy.ts")
    public static boolean stopAndDeleteJobForGraphQLSchedulerCypressTest(@GraphQLName("jobName") @GraphQLDescription("The name of the test job") @GraphQLNonNull String jobName) throws SchedulerException {
        SchedulerService schedulerService = ServicesRegistry.getInstance().getSchedulerService();
        schedulerService.getScheduler().getSchedulerListeners();

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
