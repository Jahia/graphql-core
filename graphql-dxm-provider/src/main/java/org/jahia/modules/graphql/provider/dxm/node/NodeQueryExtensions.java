/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

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

        private String workspace;

        private Workspace(String workspace) {
            this.workspace = workspace;
        }

        /**
         * @return corresponding workspace name
         */
        public String getValue() {
            return workspace;
        }
    }


    /**
     * Root for all JCR queries.
     * @param workspace the JCR workspace name for the query 
     * @return the root query object
     */
    @GraphQLField
    @GraphQLName("jcr")
    @GraphQLNonNull
    @GraphQLDescription("JCR Queries")
    public static GqlJcrQuery getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either EDIT, LIVE, or null to use EDIT by default") Workspace workspace) {
        return new GqlJcrQuery(workspace != null ? workspace : Workspace.EDIT);
    }

    /**
     * Root for all admin queries.
     * @return admin query object.
     */
    @GraphQLField
    @GraphQLName("admin")
    @GraphQLDescription("Admin Queries")
    public static GqlAdminQuery getAdmin()  {
        return new GqlAdminQuery();
    }

}
