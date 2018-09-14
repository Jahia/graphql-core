package org.jahia.test.graphql.scheduler;

import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class TestJob extends BackgroundJob {
    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        // fake doing something
        Thread.sleep(2000);
    }

    public static JobDetail createTestJob() {
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("foo", "bar");
        JobDetail job = BackgroundJob.createJahiaJob("Test job", TestJob.class);
        job.getJobDataMap().putAll(jobData);
        return job;
    }
}