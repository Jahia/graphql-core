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

import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.workflow.WorkflowService;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;

/**
 * Extensions to retrieve workflow and task data.
 * 
 * @author Sergiy Shyrkov
 */
@GraphQLTypeExtension(GqlJcrQuery.class)
public class WorkflowJCRQueryExtensions {

    @GraphQLField
    @GraphQLName("activeWorkflowTaskCountForUser")
    @GraphQLDescription("Retrieves the number of active workflow tasks for the current user")
    public static int getActiveWorkflowTaskCountForUser() {
        return BundleUtils.getOsgiService(WorkflowService.class, null)
                .getTasksForUser(JCRSessionFactory.getInstance().getCurrentUser(), null).size();
    }

}
