package org.jahia.modules.graphql.provider.dxm.workflow;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.modules.graphql.provider.dxm.user.GqlUser;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.workflow.WorkflowService;
import org.jahia.services.workflow.WorkflowTask;

import java.util.Calendar;

@GraphQLName("Task")
public class GqlTask {
    private WorkflowTask task;

    public GqlTask(WorkflowTask task) {
        this.task = task;
    }

    public WorkflowTask getTask() {
        return task;
    }

    @GraphQLField
    @GraphQLDescription("Task id")
    public String getId() {
        return task.getId();
    }

    @GraphQLField
    @GraphQLDescription("Task name")
    public String getName() {
        return task.getName();
    }

    @GraphQLField
    @GraphQLDescription("Task assignee")
    public GqlUser getAssignee() {
        if (task.getAssignee() != null) {
            return new GqlUser(task.getAssignee());
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Creation time")
    public String getCreationTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(task.getCreateTime());
        return ISO8601.format(calendar);
    }

    @GraphQLField
    @GraphQLDescription("Due date")
    public String getDueDate() {
        if (task.getDueDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(task.getDueDate());
            return ISO8601.format(calendar);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Parent workflow")
    public GqlWorkflow getWorkflow() {
        return new GqlWorkflow(BundleUtils.getOsgiService(WorkflowService.class, null).getWorkflow(task.getProvider(), task.getProcessId(), null));
    }

}
