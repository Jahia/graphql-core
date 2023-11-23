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
package org.jahia.test.graphql;

import com.king.platform.net.http.*;
import com.king.platform.net.http.netty.NettyHttpClientBuilder;
import org.jahia.bin.Jahia;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.test.graphql.scheduler.TestJob;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class GraphQLSchedulerTest extends GraphQLTestSupport {

    private static SchedulerService schedulerService;

    static Logger logger = LoggerFactory.getLogger(GraphQLSchedulerTest.class);

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        schedulerService = ServicesRegistry.getInstance().getSchedulerService();
    }

    @Test
    public void testJobSubscription() throws Exception {

        NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder();
        HttpClient httpClient = nettyHttpClientBuilder.createHttpClient();
        httpClient.start();

        List<JSONObject> jobDatas = new ArrayList<>();
        JobDetail testJob = TestJob.createTestJob();

        String subscription = "subscription backgroundJobSubscription {\n" +
                                    "backgroundJobSubscription(filterByNames:[\"" + testJob.getName() + "\"]) {\n" +
                                        "group\n" +
                                        "name\n" +
                                        "duration\n" +
                                        "siteKey\n" +
                                        "userKey\n" +
                                        "jobStatus\n" +
                                        "jobState\n" +
                                        "jobLongProperty(name:\"duration\")\n" +
                                        "foo:jobStringProperty(name:\"foo\")\n" +
                                    "}\n" +
                                "}";

        String url = (getBaseServerURL() + Jahia.getContextPath() + "/modules/graphqlws").replaceFirst("http", "ws");
        WebSocketClient webSocketClient = httpClient.createWebSocket(url)
                .idleTimeoutMillis(10000)
                .totalRequestTimeoutMillis(10000)
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("root:root1234".getBytes()))
                .addHeader("Origin", getBaseServerURL())
                .addHeader("Sec-Websocket-Protocol", "graphql-ws")
                .build().build();
        webSocketClient.addListener(new MyWebSocketMessageListener(testJob, jobDatas));
        webSocketClient.connect().get();

        JSONObject message = new JSONObject();
        message.put("id", "1");
        message.put("type", "start");
        JSONObject payload = new JSONObject();
        payload.put("variables", new JSONObject());
        payload.put("operationName", "backgroundJobSubscription");
        payload.put("query", subscription);
        message.put("payload", payload);
        webSocketClient.sendTextMessage(message.toString()).get();
        // adding a sleep just in case started payload is being called after scheduling job
        Thread.sleep(1000);

        schedulerService.scheduleJobNow(testJob);
        Thread.sleep(1000);

        webSocketClient.awaitClose();

        Assert.assertEquals(2, jobDatas.size());

        Assert.assertEquals("STARTED", jobDatas.get(0).getString("jobState"));
        Assert.assertEquals("EXECUTING", jobDatas.get(0).getString("jobStatus"));
        Assert.assertEquals(-1L, jobDatas.get(0).getLong("duration"));
        Assert.assertTrue(jobDatas.get(0).isNull("jobLongProperty"));
        Assert.assertEquals("bar", jobDatas.get(0).getString("foo"));

        Assert.assertEquals("FINISHED", jobDatas.get(1).getString("jobState"));
        Assert.assertEquals("SUCCESSFUL", jobDatas.get(1).getString("jobStatus"));
        Assert.assertTrue(jobDatas.get(1).getLong("duration") >= 500);
        Assert.assertTrue(jobDatas.get(1).getLong("jobLongProperty") >= 500);
        Assert.assertEquals("bar", jobDatas.get(0).getString("foo"));
    }

    private static class MyWebSocketMessageListener implements WebSocketMessageListenerAdapter {
        private final JobDetail testJob;
        private final List<JSONObject> jobDatas;
        private WebSocketConnection connection;

        public MyWebSocketMessageListener(JobDetail testJob, List<JSONObject> jobDatas) {
            this.testJob = testJob;
            this.jobDatas = jobDatas;
        }

        @Override
        public void onConnect(WebSocketConnection connection) {
            this.connection = connection;
        }

        @Override
        public void onTextMessage(String message) {
            try {
                JSONObject jobData = new JSONObject(message).getJSONObject("payload").getJSONObject("data").getJSONObject("backgroundJobSubscription");
                if (jobData.getString("name").equals(testJob.getName())) {
                    logger.info("Adding job data: {}", jobData.toString());
                    jobDatas.add(jobData);
                    if (jobData.getString("jobState").equals("FINISHED")) {
                        connection.sendCloseFrame();
                    }
                }
            } catch (JSONException e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}
