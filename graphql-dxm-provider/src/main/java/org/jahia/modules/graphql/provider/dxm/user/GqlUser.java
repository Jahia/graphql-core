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

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.data.viewhelper.principal.PrincipalViewHelper;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldGroupingInput;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.modules.graphql.provider.dxm.site.GqlJcrSite;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@GraphQLName("User")
@GraphQLDescription("GraphQL representation of a Jahia user")
public class GqlUser implements GqlPrincipal {
    private final JahiaUser user;

    @Inject
    @GraphQLOsgiService
    private JahiaGroupManagerService groupManagerService;

    @Inject
    @GraphQLOsgiService
    private JCRSessionFactory jcrSessionFactory;

    public GqlUser(JahiaUser jahiaUser) {
        this.user = jahiaUser;
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDeprecate
    @GraphQLDescription("User name")
    @Override
    public String getName() {
        return user.getName();
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Username of the user")
    public String getUsername() {
        return user.getName();
    }

    @GraphQLField
    @GraphQLDescription("First name of the user")
    public String getFirstname() {
        return user.getProperty("j:firstName");
    }

    @GraphQLField
    @GraphQLDescription("Last name of the user")
    public String getLastname() {
        return user.getProperty("j:lastName");
    }

    @GraphQLField
    @GraphQLDescription("Email of the user")
    public String getEmail() {
        return user.getProperty("j:email");
    }

    @GraphQLField
    @GraphQLDescription("User organization")
    public String getOrganization() {
        return user.getProperty("j:organization");
    }

    @GraphQLField
    @GraphQLDescription("Preferred language by the user")
    public String getLanguage() {
        return user.getProperty("preferredLanguage");
    }

    @GraphQLField
    @GraphQLDescription("Displays if user is locked")
    public boolean getLocked() {
        return Boolean.parseBoolean(user.getProperty("j:accountLocked"));
    }

    @GraphQLField
    @GraphQLDescription("Full display name")
    public String getDisplayName() {
        return PrincipalViewHelper.getFullName(user);
    }

    @GraphQLField
    @GraphQLDescription("User property")
    public String getProperty(@GraphQLName("name") @GraphQLNonNull @GraphQLDescription("The name of the property") String name) {
        return user.getProperty(name);
    }

    @GraphQLField
    @GraphQLDescription("Site where the user is defined")
    public GqlJcrSite getSite() throws RepositoryException {
        return new GqlJcrSite(jcrSessionFactory.getCurrentUserSession().getNode(user.getLocalPath()).getResolveSite());
    }

    @GraphQLField
    @GraphQLDescription("Is this principal member of the specified group")
    public boolean isMemberOf(@GraphQLName("group") @GraphQLDescription("Target group") String group,
                              @GraphQLName("site") @GraphQLDescription("Site where the group is defined") String site) {
        JCRGroupNode groupNode = groupManagerService.lookupGroup(site, group);
        if (groupNode == null) {
            return false;
        }
        return groupNode.isMember(user.getLocalPath());
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("List of groups this principal belongs to")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlGroup> getGroupMembership(@GraphQLName("site") @GraphQLDescription("Return only groups which belong to this site") String site,
                                                        @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                        @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                        @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                        DataFetchingEnvironment environment) {
        return GqlPrincipal.getGroupMembership(user.getLocalPath(), site, fieldFilter, fieldSorter, fieldGrouping, environment, groupManagerService);
    }

    @GraphQLField
    @GraphQLDescription("Get the corresponding JCR node")
    public GqlJcrNode getNode() throws RepositoryException {
        return SpecializedTypesHandler.getNode(jcrSessionFactory.getCurrentUserSession().getNode(user.getLocalPath()));
    }
}
