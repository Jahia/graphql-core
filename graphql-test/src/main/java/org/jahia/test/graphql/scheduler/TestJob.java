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
package org.jahia.test.graphql.scheduler;

import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestJob extends BackgroundJob {

    static Logger logger = LoggerFactory.getLogger(TestJob.class);

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        logger.info("Executing Jahia test job...");
        Thread.sleep(500);
    }

    public static JobDetail createTestJob() {
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("foo", "bar");
        JobDetail job = BackgroundJob.createJahiaJob("Test job", TestJob.class);
        job.getJobDataMap().putAll(jobData);
        return job;
    }
}
