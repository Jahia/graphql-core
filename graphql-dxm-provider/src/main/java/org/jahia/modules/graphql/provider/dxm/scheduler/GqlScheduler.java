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

import graphql.annotations.annotationTypes.GraphQLDeprecate;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.scheduler.jobs.GqlBackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.quartz.SchedulerException;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLDescription("Scheduler object which allows to access to background jobs")
public class GqlScheduler {

    @Inject
    @GraphQLOsgiService
    SchedulerService schedulerService;

    public GqlScheduler() {
    }

    @GraphQLField
    @GraphQLName("jobs")
    @GraphQLDescription("List of active jobs (deprecated)")
    @GraphQLDeprecate("Use paginatedJobs instead")
    public List<GqlBackgroundJob> getJobs(
    ) throws SchedulerException {
        return schedulerService
                .getAllJobs()
                .stream()
                .map(job -> new GqlBackgroundJob(job, GqlBackgroundJob.GqlBackgroundJobState.STARTED))
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLName("paginatedJobs")
    @GraphQLDescription("List of active jobs (paginated)")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlBackgroundJob> getPaginatedJobs(
            @GraphQLName("group") @GraphQLDescription("The group jobs belong to") String group,
            @GraphQLName("includeStatuses") @GraphQLDescription("Include jobs with these statuses") List<GqlBackgroundJob.GqlBackgroundJobStatus> includeStatuses,
            @GraphQLName("excludeStatuses") @GraphQLDescription("Exclude jobs with these statuses") List<GqlBackgroundJob.GqlBackgroundJobStatus> excludeStatuses,
            DataFetchingEnvironment environment
    ) throws SchedulerException {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        List<GqlBackgroundJob> jobs = schedulerService
                .getAllJobs()
                .stream()
                .filter(job -> group == null || group.equals(job.getGroup()))
                .map(job -> new GqlBackgroundJob(job, GqlBackgroundJob.GqlBackgroundJobState.STARTED))
                .filter(job -> includeStatuses == null || includeStatuses.contains(job.getJobStatus()))
                .filter(job -> excludeStatuses == null || !excludeStatuses.contains(job.getJobStatus()))
                .collect(Collectors.toList());
        return PaginationHelper.paginate(jobs, t -> PaginationHelper.encodeCursor(t.getName()), arguments);
    }
}


