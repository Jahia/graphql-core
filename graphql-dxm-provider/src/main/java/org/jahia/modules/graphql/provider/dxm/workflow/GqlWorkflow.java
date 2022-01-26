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
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.services.workflow.Workflow;

import java.util.Calendar;

@GraphQLName("Workflow")
public class GqlWorkflow {
    private Workflow workflowProcess;

    public GqlWorkflow(Workflow workflowProcess) {
        this.workflowProcess = workflowProcess;
    }

    @GraphQLField
    @GraphQLName("startUser")
    public String getStartUser() {
        return workflowProcess.getStartUser();
    }

    @GraphQLField
    public String getCreationTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(workflowProcess.getStartTime());
        return ISO8601.format(calendar);
    }

    @GraphQLField
    public String getDueDate() {
        if (workflowProcess.getDuedate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(workflowProcess.getDuedate());
            return ISO8601.format(calendar);
        }
        return null;
    }

}
