import org.jahia.services.scheduler.SchedulerService
import org.jahia.registries.ServicesRegistry
import org.jahia.services.scheduler.BackgroundJob
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.jahia.osgi.FrameworkService

import java.lang.reflect.Method

//import org.jahia.bundles.filters.maintenance.MaintenanceFilter

//Map<String, Object> jobData = new HashMap<>();
//jobData.put("foo", "bar");
//load TestJob from OSGI from its name


//
var ctx = FrameworkService.getBundleContext()
var serviceReference = ctx.getServiceReference("org.jahia.test.graphql.scheduler.TestJob")
ctx.getService(serviceReference)
Method method = ctx.getService(serviceReference).getClass().getMethod("schedule",String.class);
method.invoke("JOB_NAME"); // pass job name as variable
//JobDetail job = BackgroundJob.createJahiaJob("Test job",serviceReference.g);
//job.setName("JOB_NAME") // pass job name as variable
//job.getJobDataMap().putAll(jobData);
//
//// schedule
//SchedulerService schedulerService = ServicesRegistry.getInstance().getSchedulerService();
//schedulerService.scheduleJobNow(job);
//
//class TestJob extends BackgroundJob {
//
//    static Logger logger = LoggerFactory.getLogger(BackgroundJob.class);
//
//    @Override
//    void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
//
//        logger.info("Executing Jahia test job...");
//        Thread.sleep(500);
//    }
//}
