package org.jahia.modules.graphql.provider.dxm.scheduler;

import io.reactivex.FlowableEmitter;
import org.apache.jackrabbit.core.security.JahiaLoginModule;
import org.jahia.bin.filters.jcr.JcrSessionFilter;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.LanguageCodeConverters;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

import java.util.function.Predicate;

import static org.jahia.services.scheduler.BackgroundJob.JOB_CURRENT_LOCALE;
import static org.jahia.services.scheduler.BackgroundJob.JOB_USERKEY;

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
            JobDetail jobDetail = context.getJobDetail();
            JobDataMap data = jobDetail.getJobDataMap();
            final JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
            try {
                String userKey = data.getString(JOB_USERKEY);
                if (userKey != null && !userKey.equals(JahiaLoginModule.SYSTEM)) {
                    JCRUserNode userNode = JahiaUserManagerService.getInstance().lookup(userKey);
                    if (userNode != null) {
                        sessionFactory.setCurrentUser(userNode.getJahiaUser());
                    }
                }
                String langKey = data.getString(JOB_CURRENT_LOCALE);
                if (langKey != null) {
                    sessionFactory.setCurrentLocale(LanguageCodeConverters.languageCodeToLocale(langKey));
                }

                obs.onNext(gqlBackgroundJob);
            } finally {
                JcrSessionFilter.endRequest();
            }
        }
    }
}
