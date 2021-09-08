/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.scheduler;

import graphql.annotations.annotationTypes.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.scheduler.SchedulerService;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
public class GqlJobSubscriptionExtension {

    static Logger logger = LoggerFactory.getLogger(GqlJobSubscriptionExtension.class);

    @GraphQLField
    @GraphQLDescription("Subscription on background jobs")
    public static Publisher<GqlBackgroundJob> backgroundJobSubscription(
            @GraphQLName("targetScheduler") @GraphQLDefaultValue(TargetSchedulerDefaultValue.class) @GraphQLDescription("The target scheduler for listening jobs") TargetScheduler targetScheduler,
            @GraphQLName("filterByGroups") @GraphQLDescription("Subscribe only to job with matching group names") List<String> groupsFilter,
            @GraphQLName("filterByNames") @GraphQLDescription("Subscribe only to job with matching names") List<String> namesFilter,
            @GraphQLName("filterByJobStatuses") @GraphQLDescription("Subscribe only to job with matching job statuses") List<GqlBackgroundJob.GqlBackgroundJobStatus> jobStatusesFilter,
            @GraphQLName("filterByJobStates") @GraphQLDescription("Subscribe only to job with matching job states") List<GqlBackgroundJob.GqlBackgroundJobState> jobStatesFilter) {

        boolean ramScheduler = targetScheduler == TargetScheduler.RAM_SCHEDULER || targetScheduler == TargetScheduler.BOTH;
        boolean scheduler = targetScheduler == TargetScheduler.SCHEDULER || targetScheduler == TargetScheduler.BOTH;

        Predicate<GqlBackgroundJob> jobFilter = gqlBackgroundJob -> (groupsFilter == null || groupsFilter.contains(gqlBackgroundJob.getGroup())) &&
                (namesFilter == null || namesFilter.contains(gqlBackgroundJob.getName())) &&
                (jobStatusesFilter == null || jobStatusesFilter.contains(gqlBackgroundJob.getJobStatus())) &&
                (jobStatesFilter == null || jobStatesFilter.contains(gqlBackgroundJob.getJobState()));

        return Flowable.create(obs -> {
            SchedulerService schedulerService = ServicesRegistry.getInstance().getSchedulerService();
            String name = UUID.randomUUID().toString();
            GqlJobListener jobListener = new GqlJobListener(name, obs, jobFilter);

            if (ramScheduler) {
                logger.info("Adding job listener {} for RAM scheduler", name);
                schedulerService.addJobListener(jobListener, true);
            }

            if (scheduler) {
                logger.info("Adding job listener {}", name);
                schedulerService.addJobListener(jobListener, false);
            }

            obs.setCancellable(() -> {
                if (ramScheduler) {
                    schedulerService.removeJobListener(name, true);
                }

                if (scheduler) {
                    schedulerService.removeJobListener(name, false);
                }
            });
        }, BackpressureStrategy.BUFFER);
    }

    /**
     * The target scheduler(s)
     */
    @GraphQLDescription("The target scheduler(s)")
    public enum TargetScheduler {
        @GraphQLDescription("Persisted scheduler will be used")
        SCHEDULER,

        @GraphQLDescription("RAM scheduler will be used")
        RAM_SCHEDULER,

        @GraphQLDescription("Both persisted and RAM schedulers will be used")
        BOTH
    }

    /**
     * Default value supplier for {@link TargetScheduler}.
     */
    public static class TargetSchedulerDefaultValue implements Supplier<Object> {

        @Override
        public GqlJobSubscriptionExtension.TargetScheduler get() {
            return TargetScheduler.BOTH;
        }
    }
}
