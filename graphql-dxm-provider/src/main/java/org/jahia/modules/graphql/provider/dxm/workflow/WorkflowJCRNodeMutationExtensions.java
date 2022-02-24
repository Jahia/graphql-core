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
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.modules.graphql.provider.dxm.node.NodeHelper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.workflow.WorkflowService;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.HashMap;

/**
 * Extensions for JCRNodeMutation
 */
@GraphQLTypeExtension(GqlJcrNodeMutation.class)
public class WorkflowJCRNodeMutationExtensions {


    private GqlJcrNodeMutation nodeMutation;

    public WorkflowJCRNodeMutationExtensions(GqlJcrNodeMutation nodeMutation) {
        this.nodeMutation = nodeMutation;
    }

    @GraphQLField
    public boolean startWorkflow(@GraphQLName("definition") String workflowDefinitionId, @GraphQLName("language") String language) {
        WorkflowService service = BundleUtils.getOsgiService(WorkflowService.class, null);
        try {
            JCRNodeWrapper node = NodeHelper.getNodeInLanguage(nodeMutation.getNode().getNode(), language);
            service.startProcess(Collections.singletonList(node.getIdentifier()), node.getSession(), StringUtils.substringAfter(workflowDefinitionId,":"), StringUtils.substringBefore(workflowDefinitionId,":"), new HashMap<>(), null);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }


}
