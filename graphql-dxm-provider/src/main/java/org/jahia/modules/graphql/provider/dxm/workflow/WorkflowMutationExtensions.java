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

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.workflow.Workflow;
import org.jahia.services.workflow.WorkflowService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extensions for JCRNodeMutation
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public class WorkflowMutationExtensions {

    @GraphQLField
    public static Collection<GqlWorkflowMutation> mutateWorkflows(@GraphQLName("definition") String workflowDefinitionId) {
        WorkflowService service = BundleUtils.getOsgiService(WorkflowService.class, null);
        List<Workflow> wfs = service.getWorkflowsForDefinition(StringUtils.substringAfter(workflowDefinitionId,":"), null);
        return wfs.stream().map(GqlWorkflowMutation::new).collect(Collectors.toList());
    }
}
