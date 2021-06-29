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
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import pl.touk.throwing.ThrowingPredicate;

import javax.inject.Inject;
import java.util.stream.Stream;

@GraphQLName("UserAdminQuery")
@GraphQLDescription("User admin queries")
public class GqlUserAdmin {

    @Inject
    @GraphQLOsgiService
    private JahiaUserManagerService userManagerService;

    @Inject
    @GraphQLOsgiService
    private JahiaGroupManagerService groupManagerService;

    @GraphQLField
    @GraphQLDescription("Get a user")
    public GqlUser getUser(@GraphQLName("username") @GraphQLDescription("User name") @GraphQLNonNull String userName,
                           @GraphQLName("site") @GraphQLDescription("Site where the user is defined") String site) {
        JCRUserNode jcrUserNode = userManagerService.lookupUser(userName, site);
        if (jcrUserNode == null) {
            return null;
        }
        return new GqlUser(jcrUserNode.getJahiaUser());
    }

    @GraphQLField
    @GraphQLDescription("Get a group")
    public GqlGroup getGroup(@GraphQLName("groupName") @GraphQLDescription("Group name") @GraphQLNonNull String groupName,
                             @GraphQLName("site") @GraphQLDescription("Site where the group is defined") String site) {
        JCRGroupNode jcrGroupNode = groupManagerService.lookupGroup(site, groupName);
        if (jcrGroupNode == null) {
            return null;
        }
        return new GqlGroup(jcrGroupNode.getJahiaGroup());
    }

    @GraphQLField
    @GraphQLDescription("Get users list")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlUser> getUsers(@GraphQLName("site") @GraphQLDescription("Return only users which belong to this site") String site,
                                             @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                             @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                             @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                             DataFetchingEnvironment environment) {
        Stream<GqlUser> userStream = userManagerService.searchUsers(null)
                .stream()
                .map(user -> new GqlUser(user.getJahiaUser()))
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));

        if (fieldSorter != null) {
            userStream = userStream.sorted(SorterHelper.getFieldComparator(fieldSorter, FieldEvaluator.forConnection(environment)));
        }

        if (fieldGrouping != null) {
            userStream = GroupingHelper.group(userStream, fieldGrouping, FieldEvaluator.forConnection(environment));
        }

        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return PaginationHelper.paginate(userStream, n -> PaginationHelper.encodeCursor(n.getName()), arguments);
    }

}
