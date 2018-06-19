/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.search;

/**
 * add searchQuery to GraphQL query structure
 *
 * @author yousria
 */

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;


@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("query nodes")
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
     * Root for all Search queries.
     * @param workspace the JCR workspace name for the query
     * @return the root query object
     */
    @GraphQLField
    @GraphQLName("searchQuery")
    @GraphQLDescription("Search Queries")
    public static GqlSearch getJcr(@GraphQLName("workspace") @GraphQLDescription("The name of the workspace to fetch the node from; either "
            + "EDIT, LIVE, or null to use EDIT by default") org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.Workspace workspace) {
        return new GqlSearch(workspace != null ? workspace : org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions.Workspace.EDIT);
    }

}