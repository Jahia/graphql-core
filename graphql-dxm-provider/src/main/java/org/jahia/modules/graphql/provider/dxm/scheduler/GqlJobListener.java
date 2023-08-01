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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;

public class GqlJobListener<T extends GqlBackgroundJob> extends JobListenerSupport {

    private String name;
    private FlowableEmitter<T> obs;
    private Predicate<T> jobFilter;
    private Class<T> clazz;

    public GqlJobListener(String name, FlowableEmitter<T> obs, Predicate<T> jobFilter, Class<T> clazz) {
        this.name = name;
        this.obs = obs;
        this.jobFilter = jobFilter;
        this.clazz = clazz;
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
        T gqlBackgroundJob = null;
        try {
            gqlBackgroundJob = getInstanceOfT();
            gqlBackgroundJob.init(context.getJobDetail(), state);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (jobFilter.test(gqlBackgroundJob)) {
            obs.onNext(gqlBackgroundJob);
        }
    }

    private T getInstanceOfT() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return this.clazz.getDeclaredConstructor().newInstance();
    }
}
