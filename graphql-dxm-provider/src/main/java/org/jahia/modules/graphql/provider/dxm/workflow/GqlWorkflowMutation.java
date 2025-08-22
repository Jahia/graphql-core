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
package org.jahia.modules.graphql.provider.dxm.workflow;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.workflow.Workflow;
import org.jahia.services.workflow.WorkflowService;

@GraphQLName("WorkflowMutation")
@GraphQLDescription("Workflow mutation operations")
public class GqlWorkflowMutation {

    private Workflow workflowProcess;

    public GqlWorkflowMutation(Workflow workflowProcess) {
        this.workflowProcess = workflowProcess;
    }

    @GraphQLField
    @GraphQLName("workflow")
    @GraphQLDescription("Get the workflow associated with this mutation")
    public GqlWorkflow getWorkflow() {
        return new GqlWorkflow(workflowProcess);
    }

    @GraphQLField
    @GraphQLDescription("Abort the workflow process")
    public boolean abortWorkflow() {
        WorkflowService service = BundleUtils.getOsgiService(WorkflowService.class, null);
        service.abortProcess(workflowProcess.getId(), workflowProcess.getProvider());
        return true;
    }
}
