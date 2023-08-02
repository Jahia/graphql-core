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

import io.reactivex.FlowableEmitter;
import org.jahia.modules.graphql.provider.dxm.scheduler.jobs.GqlBackgroundJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

import java.util.function.Predicate;

public class GqlJobListener extends JobListenerSupport {

    private String name;
    private FlowableEmitter<GqlBackgroundJob> obs;
    private Predicate<GqlBackgroundJob> jobFilter;

    public GqlJobListener(String name, FlowableEmitter<GqlBackgroundJob> obs, Predicate<GqlBackgroundJob> jobFilter) {
        this.name = name;
        this.obs = obs;
        this.jobFilter = jobFilter;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        submitJobEvent(context, GqlBackgroundJob.GqlBackgroundJobState.STARTED);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        submitJobEvent(context, GqlBackgroundJob.GqlBackgroundJobState.VETOED);
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        submitJobEvent(context, GqlBackgroundJob.GqlBackgroundJobState.FINISHED);
    }

    private void submitJobEvent(JobExecutionContext context, GqlBackgroundJob.GqlBackgroundJobState state) {
        GqlBackgroundJob gqlBackgroundJob = new GqlBackgroundJob(context.getJobDetail(), state);
        if (jobFilter.test(gqlBackgroundJob)) {
            obs.onNext(gqlBackgroundJob);
        }
    }
}
