/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.data.viewhelper.principal.PrincipalViewHelper;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupManagerService;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@GraphQLName("Group")
@GraphQLDescription("GraphQL representation of a Jahia group")
public class GqlGroup implements GqlPrincipal {
    private final JahiaGroup group;

    @Inject
    @GraphQLOsgiService
    private JahiaGroupManagerService groupManagerService;

    @Inject
    @GraphQLOsgiService
    private JCRSessionFactory jcrSessionFactory;

    public GqlGroup(JahiaGroup jahiaGroup) {
        this.group = jahiaGroup;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Group name")
    public String getName() {
        return group.getName();
    }

    @GraphQLField
    @GraphQLDescription("Full display name")
    public String getDisplayName() {
        return PrincipalViewHelper.getFullName(group);
    }

    @GraphQLField
    @GraphQLDescription("Group property")
    public String getProperty(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property") String name) {
        return group.getProperty(name);
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Group members")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlPrincipal> getMembers(DataFetchingEnvironment environment) {
        Collection<JCRNodeWrapper> nodes = groupManagerService.lookupGroupByPath(group.getLocalPath()).getMembers();
        List<GqlPrincipal> result = new ArrayList<>();
        for (JCRNodeWrapper node : nodes) {
            if (node instanceof JCRUserNode) {
                result.add(new GqlUser(((JCRUserNode) node).getJahiaUser()));
            } else if (node instanceof JCRGroupNode) {
                result.add(new GqlGroup(((JCRGroupNode) node).getJahiaGroup()));
            }
        }
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return PaginationHelper.paginate(result, n -> PaginationHelper.encodeCursor(n.getName()), arguments);
    }

    @GraphQLField
    @GraphQLDescription("Is this principal member of the specified group")
    public boolean isMemberOf(@GraphQLName("group") String group,
                              @GraphQLName("site") @GraphQLDescription("Site where the group is defined") String site) {
        JCRGroupNode groupNode = groupManagerService.lookupGroup(site, group);
        if (groupNode == null) {
            return false;
        }
        return groupNode.isMember(this.group.getLocalPath());
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("List of groups this principal belongs to")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlGroup> getGroupMembership(DataFetchingEnvironment environment) {
        List<String> paths = groupManagerService.getMembershipByPath(group.getLocalPath());
        Stream<GqlGroup> groups = paths.stream().map(path -> new GqlGroup(groupManagerService.lookupGroupByPath(path).getJahiaGroup()));

        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return PaginationHelper.paginate(groups, n -> PaginationHelper.encodeCursor(n.getName()), arguments);
    }

    @GraphQLField
    @GraphQLDescription("Get the corresponding JCR node")
    public GqlJcrNode getNode() throws RepositoryException {
        return SpecializedTypesHandler.getNode(jcrSessionFactory.getCurrentUserSession().getNode(group.getLocalPath()));
    }
}
