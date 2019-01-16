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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

/**
 * Info about a node to be reproduced at (moved or copied to) another parent node.
 */
@GraphQLName("CarriedJCRNode")
@GraphQLDescription("Info about a node to be reproduced at (moved or copied to) another parent node")
public class GqlJcrReproducibleNodeInput {

    private String pathOrId;
    private String destParentPathOrId;
    private String destName;

    /**
     * Create an instance of the input object.
     *
     * @param pathOrId Path or UUID of the node to be copied/moved
     * @param destParentPathOrId Path or UUID of the destination parent node to copy/move the node to
     * @param destName The name of the node at the new location or null if its current name should be preserved
     */
    public GqlJcrReproducibleNodeInput(
        @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the node to be copied/moved") String pathOrId,
        @GraphQLName("destParentPathOrId") @GraphQLNonNull @GraphQLDescription("Path or UUID of the destination parent node to copy/move the node to") String destParentPathOrId,
        @GraphQLName("destName") @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved") String destName
    ) {
        this.pathOrId = pathOrId;
        this.destParentPathOrId = destParentPathOrId;
        this.destName = destName;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Path or UUID of the node to be copied/moved")
    public String getPathOrId() {
        return pathOrId;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Path or UUID of the destination parent node to copy/move the node to")
    public String getDestParentPathOrId() {
        return destParentPathOrId;
    }

    @GraphQLField
    @GraphQLDescription("The name of the node at the new location or null if its current name should be preserved")
    public String getDestName() {
        return destName;
    }
}
