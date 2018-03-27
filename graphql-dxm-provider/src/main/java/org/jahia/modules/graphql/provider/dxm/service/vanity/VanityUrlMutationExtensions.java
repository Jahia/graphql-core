/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.*;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.seo.jcr.VanityUrlManager;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extensions for JCRNodeMutation
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public class VanityUrlMutationExtensions extends GqlJcrMutationSupport {

    /**
     * Mutate multiple vanities at the same time
     * @param pathsOrIds paths or ids of vanities node to mutate
     * @return the Mutation
     * @throws GqlJcrWrongInputException In case the parameter is not a vanity node
     */
    @GraphQLField
    public static GqlJcrVanityUrlMutation mutateVanityUrls(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or ids ofs the vanities to mutate") Collection<String> pathsOrIds) throws GqlJcrWrongInputException {
        JCRSessionWrapper session = getSession();

        return new GqlJcrVanityUrlMutation(new HashSet<>(pathsOrIds).stream().map((String pathOrId) -> {
            JCRNodeWrapper vanityUrlNode = getNodeFromPathOrId(session, pathOrId);
            try {
                if (!vanityUrlNode.isNodeType(VanityUrlManager.JAHIANT_VANITYURL)) {
                    throw new GqlJcrWrongInputException("Node with uuid: " + vanityUrlNode.getIdentifier() + " is not a vanity url mapping node");
                }
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e);
            }
            return vanityUrlNode;
        }).collect(Collectors.toSet()), session);
    }

    private static JCRSessionWrapper getSession() {
        try {
            return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
