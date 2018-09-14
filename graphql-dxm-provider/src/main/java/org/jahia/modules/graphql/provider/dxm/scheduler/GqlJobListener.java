package org.jahia.modules.graphql.provider.dxm.scheduler;

import io.reactivex.FlowableEmitter;
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
