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


import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDetail;

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
    public String getJobStringProperty(@GraphQLName("name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getString(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobLongProperty")
    @GraphQLDescription("The job (Long) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Long getJobLongProperty(@GraphQLName("name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getLongValue(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobIntegerProperty")
    @GraphQLDescription("The job (Int) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Integer getJobIntegerProperty(@GraphQLName("name") String name) {
        if (jobDetail.getJobDataMap().containsKey(name)) {
            return jobDetail.getJobDataMap().getIntValue(name);
        }
        return null;
    }

    @GraphQLField
    @GraphQLName("jobBooleanProperty")
    @GraphQLDescription("The job (Boolean) property that correspond to the given name. The returned value will be null in case the job doesn't have the property")
    public Boolean getJobBooleanProperty(@GraphQLName("name") String name) {
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


