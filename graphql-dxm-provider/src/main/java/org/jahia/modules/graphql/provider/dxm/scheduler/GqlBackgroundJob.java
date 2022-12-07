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
import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDetail;

@GraphQLDescription("Background job")
public class GqlBackgroundJob {
    private JobDetail jobDetail;
    private GqlBackgroundJobStatus jobStatus;
    private GqlBackgroundJobState jobState;

    public GqlBackgroundJob(JobDetail jobDetail, GqlBackgroundJobState state) {
        this.jobDetail = jobDetail;
        this.jobStatus = GqlBackgroundJobStatus.valueOf(jobDetail.getJobDataMap().getString(BackgroundJob.JOB_STATUS).toUpperCase());
        this.jobState = state;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("The job name")
    public String getName() {
        return jobDetail.getName();
    }

    @GraphQLField
    @GraphQLName("group")
    @GraphQLDescription("The job group name")
    public String getGroup() {
        return jobDetail.getGroup();
    }

    @GraphQLField
    @GraphQLName("jobStatus")
    @GraphQLDescription("The job status")
    public GqlBackgroundJobStatus getJobStatus() {
        return jobStatus;
    }

    @GraphQLField
    @GraphQLName("jobState")
    @GraphQLDescription("The job state is different from the status, it reflect the last action done on the job instance (Started, Vetoed, Finished)")
    public GqlBackgroundJobState getJobState() {
        return jobState;
    }

    @GraphQLField
    @GraphQLName("duration")
    @GraphQLDescription("The amount of time the job ran for (in milliseconds). The returned value will be -1 until the job has actually completed")
    public Long getDuration() {
        if (jobDetail.getJobDataMap().containsKey(BackgroundJob.JOB_DURATION)) {
            return jobDetail.getJobDataMap().getLongFromString(BackgroundJob.JOB_DURATION);
        }
        return -1L;
    }

    @GraphQLField
    @GraphQLName("userKey")
    @GraphQLDescription("The user key. The returned value will be null in case the job doesn't have associated user key")
    public String getUserKey() {
        if (jobDetail.getJobDataMap().containsKey(BackgroundJob.JOB_USERKEY)) {
            return jobDetail.getJobDataMap().getString(BackgroundJob.JOB_USERKEY);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("siteKey")
    @GraphQLDescription("The site key. The returned value will be null in case the job doesn't have associated site key")
    public String getSiteKey() {
        if (jobDetail.getJobDataMap().containsKey(BackgroundJob.JOB_SITEKEY)) {
            return jobDetail.getJobDataMap().getString(BackgroundJob.JOB_SITEKEY);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobStringProperty")
    @GraphQLDescription("The job (String) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public String getJobStringProperty(@GraphQLName("name") @GraphQLDescription("The job name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getString(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobLongProperty")
    @GraphQLDescription("The job (Long) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Long getJobLongProperty(@GraphQLName("name") @GraphQLDescription("The job name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getLongValue(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobIntegerProperty")
    @GraphQLDescription("The job (Int) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Integer getJobIntegerProperty(@GraphQLName("name") @GraphQLDescription("The job name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getIntValue(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobBooleanProperty")
    @GraphQLDescription("The job (Boolean) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Boolean getJobBooleanProperty(@GraphQLName("name") @GraphQLDescription("The job name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getBoolean(name);
        }
        return null;
    }

    public enum GqlBackgroundJobStatus {
        ADDED, SCHEDULED, EXECUTING, SUCCESSFUL, FAILED, CANCELED
    }

    public enum GqlBackgroundJobState {
        STARTED, VETOED, FINISHED
    }
}


