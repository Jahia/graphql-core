package org.jahia.modules.graphql.provider.dxm.scheduler.jobs.publicationjob;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.scheduler.jobs.GqlBackgroundJob;

@GraphQLTypeExtension(GqlBackgroundJob.class)
@GraphQLDescription("Extension for the background job")
public class GqlBackgroundJobExtension {

    GqlBackgroundJob gqlBackgroundJob;

    public GqlBackgroundJobExtension(GqlBackgroundJob gqlBackgroundJob) {
        this.gqlBackgroundJob = gqlBackgroundJob;
    }

    @GraphQLField
    @GraphQLName("publicationJob")
    @GraphQLDescription("GraphQL representation of publication job")
    public GqlPublicationBackgroundJob getPublicationJob() {
        if ("PublicationJob".equals(gqlBackgroundJob.getGroup())) {
            return new GqlPublicationBackgroundJob(gqlBackgroundJob.getJobDetail(), gqlBackgroundJob.getJobState());
        }

        return null;
    }
}
