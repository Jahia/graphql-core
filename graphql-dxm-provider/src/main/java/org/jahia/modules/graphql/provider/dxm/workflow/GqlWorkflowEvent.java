/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.workflow;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import io.reactivex.FlowableEmitter;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.workflow.WorkflowService;
import org.jahia.services.workflow.WorkflowTask;

import java.util.List;
import java.util.stream.Collectors;

public class GqlWorkflowEvent {
    private WorkflowService workflowService;
    private GqlWorkflow startedWorkflow;
    private GqlWorkflow endedWorkflow;
    private GqlTask createdTask;
    private GqlTask endedTask;

    public GqlWorkflowEvent(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GraphQLField
    @GraphQLDescription("Workflow that has just been started")
    public GqlWorkflow getStartedWorkflow() {
        return startedWorkflow;
    }

    public void setStartedWorkflow(GqlWorkflow startedWorkflow) {
        this.startedWorkflow = startedWorkflow;
    }

    @GraphQLField
    @GraphQLDescription("Workflow that has just been ended")
    public GqlWorkflow getEndedWorkflow() {
        return endedWorkflow;
    }

    public void setEndedWorkflow(GqlWorkflow endedWorkflow) {
        this.endedWorkflow = endedWorkflow;
    }

    @GraphQLField
    @GraphQLDescription("Task that has just been created")
    public GqlTask getCreatedTask() {
        return createdTask;
    }

    public void setCreatedTask(GqlTask createdTask) {
        this.createdTask = createdTask;
    }

    @GraphQLField
    @GraphQLDescription("Task that has just been ended")
    public GqlTask getEndedTask() {
        return endedTask;
    }

    public void setEndedTask(GqlTask endedTask) {
        this.endedTask = endedTask;
    }

    @GraphQLField
    @GraphQLDescription("Number of tasks for current user")
    public Integer activeWorkflowTaskCountForUser() {
        List<WorkflowTask> tasksForUser = workflowService.getTasksForUser(JCRSessionFactory.getInstance().getCurrentUser(), null);
        if (endedTask != null) {
            tasksForUser = tasksForUser.stream().filter(workflowTask -> !workflowTask.getId().equals(endedTask.getTask().getId())).collect(Collectors.toList());
        }
        return tasksForUser.size();
    }

}
