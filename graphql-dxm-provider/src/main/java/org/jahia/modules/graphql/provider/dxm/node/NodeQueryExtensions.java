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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;

import java.util.function.Supplier;

/**
 * A query extension that adds a possibility to fetch nodes by their UUIDs, paths, or via an SQL2/Xpath query.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("A query extension that adds a possibility to fetch nodes by their UUIDs, paths, or via an SQL2/Xpath query")
public class NodeQueryExtensions {

    /**
     * JCR workspace to use for the operations.
     */
    @GraphQLDescription("JCR workspace to use for the operations")
    public enum Workspace {

        /**
         * Edit workspace
         */
        @GraphQLDescription("Edit workspace")
        EDIT(Constants.EDIT_WORKSPACE),

        /**
         * Live workspace
         */
        @GraphQLDescription("Live workspace")
        LIVE(Constants.LIVE_WORKSPACE);

        private final String workspace;

        Workspace(String workspace) {
            this.workspace = workspace;
        }

        /**
         * @return corresponding workspace name
         */
        public String getValue() {
            return workspace;
        }

        public static NodeQueryExtensions.Workspace fromName(String workspace) {
            if (workspace == null) {
                return null;
            }
            if (NodeQueryExtensions.Workspace.EDIT.toString().equalsIgnoreCase(workspace)) {
                return NodeQueryExtensions.Workspace.EDIT;
            }
            if (NodeQueryExtensions.Workspace.LIVE.toString().equalsIgnoreCase(workspace)) {
                return NodeQueryExtensions.Workspace.LIVE;
            }
            return null;
        }

        public static class DefaultWorkspaceSupplier implements Supplier<Object> {

            @Override
            public Workspace get() {
                return EDIT;
            }
        }
    }


    /**
     * Root for all JCR queries.
     *
     * @param workspace the JCR workspace name for the query
     * @return the root query object
     */
    @GraphQLField
    @GraphQLName("jcr")
    @GraphQLNonNull
    @GraphQLDescription("JCR Queries")
    public static GqlJcrQuery getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; "
            + "either EDIT, LIVE, or null to use EDIT by default") @GraphQLDefaultValue(value = Workspace.DefaultWorkspaceSupplier.class) Workspace workspace,
                                     @GraphQLName(JcrVersionSessions.VERSION_AT) @GraphQLDescription("Reads the EDIT workspace as it was at this date, "
            + "in milliseconds since the epoch. Each node shows its last version at or before this date, and a node with no version shows live. "
            + "The content-versioning module and Jahia 8.2.4 supply the versions. The query is read-only, it selects the jcr field alone, "
            + "and it is the only operation of its request.") Long versionAt,
                                     @GraphQLName(JcrVersionSessions.VERSION_OPERATION) @GraphQLDescription("Reads the EDIT workspace as one operation "
            + "of the content-versioning module left it, by the UUID of the operation. An operation that no version you can read carries "
            + "fails the query. The rules of versionAt apply, and the two arguments exclude each other.") String versionOperation,
                                     DataFetchingEnvironment environment) {
        ContextUtil.setJcrLiveOperationHeaderIfNeeded(workspace, OperationDefinition.Operation.QUERY, environment.getGraphQlContext());
        if (versionAt == null && versionOperation == null) {
            return new GqlJcrQuery(workspace);
        }
        if (!JcrVersionSessions.getInstance().isOpened(environment.getGraphQlContext())) {
            throw new DataFetchingException("The version sessions of this query are not open");
        }
        return new GqlJcrQuery(workspace, true);
    }

    /**
     * Root for all JCR queries, without a version. The schema exposes the method with the version arguments.
     *
     * @param workspace   the JCR workspace name for the query
     * @param environment the environment of the field
     * @return the root query object
     */
    public static GqlJcrQuery getJcr(Workspace workspace, DataFetchingEnvironment environment) {
        return getJcr(workspace, null, null, environment);
    }
}
