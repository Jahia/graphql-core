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

import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestJob extends BackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(TestJob.class);

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws InterruptedException {
        Thread.sleep(500);
        logger.info("Executing test job with name: {}", jobExecutionContext.getJobDetail().getKey().getName());
    }

    static JobDetail createTestJobDetail(String jobName) {
        JobDetail jobDetail = BackgroundJob.createJahiaJob("Test background job", TestJob.class);
        jobDetail.setName(jobName);
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("foo", "bar");
        jobDetail.getJobDataMap().putAll(jobData);
        return jobDetail;
    }
}
