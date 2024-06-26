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

import graphql.annotations.annotationTypes.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.util.BeanWrapper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.workflow.*;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
public class GqlWorkflowSubscriptionExtension {

    @GraphQLField
    @GraphQLDescription("Subscription on workflows")
    public static Publisher<GqlWorkflowEvent> workflowEvent(DataFetchingEnvironment environment) {
        String userKey = JCRSessionFactory.getInstance().getCurrentUser().getUserKey();
        return Flowable.create(obs -> {
            WorkflowService workflowService = BundleUtils.getOsgiService(WorkflowService.class, null);
            Set<String> filters = environment.getSelectionSet().getFields().stream()
                    .map(SelectedField::getName)
                    .collect(Collectors.toSet());
            GqlWfListener wfListener = new GqlWfListener(workflowService, obs, filters, userKey);
            workflowService.addWorkflowListener(wfListener);

            obs.setCancellable(() -> {
                BeanWrapper.wrap(workflowService.getObservationManager()).get("listeners").unwrap(List.class).remove(wfListener);
            });
        }, BackpressureStrategy.BUFFER);
    }

    public static class GqlWfListener extends WorkflowListener {

        private final WorkflowService workflowService;
        private final FlowableEmitter<GqlWorkflowEvent> obs;
        private final Set<String> filters;

        private final String userKey;

        public GqlWfListener(WorkflowService workflowService, FlowableEmitter<GqlWorkflowEvent> obs, Set<String> filters, String userKey) {
            this.workflowService = workflowService;
            this.obs = obs;
            this.filters = filters;
            // the current user for session when the subscription is created
            this.userKey = userKey;

            if (filters.contains("activeWorkflowTaskCountForUser")) {
                obs.onNext(new GqlWorkflowEvent(workflowService, userKey));
            }

        }

        @Override
        public void workflowStarted(Workflow workflow) {
            if (filters.contains("startedWorkflow")) {
                GqlWorkflowEvent t = new GqlWorkflowEvent(workflowService, userKey);
                t.setStartedWorkflow(new GqlWorkflow(workflow));
                obs.onNext(t);
            }
        }

        @Override
        public void workflowEnded(HistoryWorkflow workflow) {
            if (filters.contains("endedWorkflow")) {
                GqlWorkflowEvent t = new GqlWorkflowEvent(workflowService, userKey);
                obs.onNext(t);
            }
        }

        @Override
        public void newTaskCreated(WorkflowTask task) {
            if (filters.contains("createdTask") || filters.contains("activeWorkflowTaskCountForUser")) {
                GqlWorkflowEvent t = new GqlWorkflowEvent(workflowService, userKey);
                t.setCreatedTask(new GqlTask(task));
                obs.onNext(t);
            }
        }

        @Override
        public void taskEnded(WorkflowTask task) {
            if (filters.contains("endedTask") || filters.contains("activeWorkflowTaskCountForUser")) {
                GqlWorkflowEvent t = new GqlWorkflowEvent(workflowService, userKey);
                t.setEndedTask(new GqlTask(task));
                obs.onNext(t);
            }
        }
    }
}
