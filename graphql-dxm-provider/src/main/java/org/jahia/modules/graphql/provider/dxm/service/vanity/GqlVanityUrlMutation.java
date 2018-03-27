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
import org.jahia.modules.graphql.provider.dxm.DXGraphQLFieldCompleter;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extensions for JCRNodeMutation
 */
@GraphQLName("VanityUrlMutation")
@GraphQLDescription("Mutations on a JCR node")
public class GqlVanityUrlMutation extends GqlJcrMutationSupport implements DXGraphQLFieldCompleter {

    /**
     * Mutate multiple vanities at the same time
     * @param pathsOrIds paths or ids of vanities node to mutate
     * @return the Mutation
     * @throws GqlJcrWrongInputException In case the parameter is not a vanity node
     */
    @GraphQLField
    public static Collection<GqlVanityUrlMappingMutation> mutateVanityUrls(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("The paths or ids ofs the vanity urls to mutate") Collection<String> pathsOrIds) throws GqlJcrWrongInputException {
        return new HashSet<>(pathsOrIds).stream().map((pathOrId) -> new GqlVanityUrlMappingMutation(getNodeFromPathOrId(getSession(), pathOrId))).collect(Collectors.toList());
    }

    private static JCRSessionWrapper getSession() {
        try {
            return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @Override
    public void completeField() {
        // flush the cache and save
        BundleUtils.getOsgiService(VanityUrlService.class, null).flushCaches();
        try {
            getSession().save();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
