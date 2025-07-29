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

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

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
    public enum Workspace implements Supplier<Object> {

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

        @Override
        public Object get() {
            return EDIT;
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
            + "either EDIT, LIVE, or null to use EDIT by default") @GraphQLDefaultValue(value = Workspace.class) Workspace workspace) {
        return new GqlJcrQuery(workspace);
    }
}
