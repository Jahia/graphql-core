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

import graphql.annotations.annotationTypes.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.scheduler.jobs.GqlBackgroundJob;
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
            @GraphQLName("filterByJobStates") @GraphQLDescription("Subscribe only to job with matching job states") List<GqlBackgroundJob.GqlBackgroundJobState> jobStatesFilter,
            @GraphQLName("filterByUserKey") @GraphQLDescription("Subscribe only to job with matching user keys") List<String> jobUserKeyFilter) {

        boolean ramScheduler = targetScheduler == TargetScheduler.RAM_SCHEDULER || targetScheduler == TargetScheduler.BOTH;
        boolean scheduler = targetScheduler == TargetScheduler.SCHEDULER || targetScheduler == TargetScheduler.BOTH;

        Predicate<GqlBackgroundJob> jobFilter = gqlBackgroundJob -> (groupsFilter == null || groupsFilter.contains(gqlBackgroundJob.getGroup())) &&
                (jobUserKeyFilter == null || jobUserKeyFilter.contains(gqlBackgroundJob.getUserKey())) &&
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
