package org.jahia.test.graphql;

import com.king.platform.net.http.HttpClient;
import com.king.platform.net.http.SseClient;
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
import org.quartz.SchedulerException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class GraphQLSchedulerTest extends GraphQLTestSupport {

    private static SchedulerService schedulerService;

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



        String url = getBaseServerURL() + Jahia.getContextPath() + "/modules/graphql?query=" + URLEncoder.encode(subscription, "UTF-8");
        SseClient sseClient = httpClient.createSSE(url)
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("root:root1234".getBytes()))
                .idleTimeoutMillis(0)
                .totalRequestTimeoutMillis(80000)
                .keepAlive(true)
                .build()
                .execute();

        sseClient.onConnect(() -> {
            try {
                schedulerService.scheduleJobNow(testJob);
            } catch (SchedulerException e) {
                Assert.fail(e.getMessage());
            }
        });

        sseClient.onEvent((lastSentId, event, data) -> {
            try {
                JSONObject jobData = new JSONObject(data).getJSONObject("data").getJSONObject("backgroundJobSubscription");
                if (jobData.getString("name").equals(testJob.getName())) {
                    jobDatas.add(jobData);
                    if (jobData.getString("jobState").equals("FINISHED")) {
                        sseClient.close();
                    }
                }
            } catch (JSONException e) {
                Assert.fail(e.getMessage());
            }
        });

        sseClient.onError(throwable -> {
            sseClient.close();
            Assert.fail(throwable.getMessage());
        });

        // wait for SSE client to close by himself
        sseClient.awaitClose();
        Assert.assertEquals(2, jobDatas.size());

        Assert.assertEquals("STARTED", jobDatas.get(0).getString("jobState"));
        Assert.assertEquals("EXECUTING", jobDatas.get(0).getString("jobStatus"));
        Assert.assertEquals(-1L, jobDatas.get(0).getLong("duration"));
        Assert.assertEquals("null", jobDatas.get(0).getString("jobLongProperty"));
        Assert.assertEquals("bar", jobDatas.get(0).getString("foo"));

        Assert.assertEquals("FINISHED", jobDatas.get(1).getString("jobState"));
        Assert.assertEquals("SUCCESSFUL", jobDatas.get(1).getString("jobStatus"));
        Assert.assertTrue(jobDatas.get(1).getLong("duration") >= 2000);
        Assert.assertTrue(jobDatas.get(1).getLong("jobLongProperty") >= 2000);
        Assert.assertEquals("bar", jobDatas.get(0).getString("foo"));
    }
}
