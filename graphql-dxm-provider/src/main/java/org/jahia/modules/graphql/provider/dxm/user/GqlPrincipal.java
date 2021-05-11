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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.site.GqlJcrSite;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import pl.touk.throwing.ThrowingPredicate;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.stream.Stream;

@GraphQLName("Principal")
@GraphQLDescription("GraphQL representation of a principal")
@GraphQLTypeResolver(PrincipalTypeResolver.class)
public interface GqlPrincipal {

    static DXPaginatedData<GqlGroup> getGroupMembership(String localPath,
                                                        String site,
                                                        FieldFiltersInput fieldFilter,
                                                        FieldSorterInput fieldSorter,
                                                        FieldGroupingInput fieldGrouping,
                                                        DataFetchingEnvironment environment, JahiaGroupManagerService groupManagerService) {

        List<String> paths = groupManagerService.getMembershipByPath(localPath);
        Stream<GqlGroup> stream = paths.stream()
                .map(path -> groupManagerService.lookupGroupByPath(path).getJahiaGroup())
                .filter(group -> !group.isHidden())
                .filter(ThrowingPredicate.unchecked(n -> site == null || n.getLocalPath().startsWith("/sites/" + site + "/")))
                .map(GqlGroup::new)
                .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));

        if (fieldSorter != null) {
            stream = stream.sorted(SorterHelper.getFieldComparator(fieldSorter, FieldEvaluator.forConnection(environment)));
        }

        if (fieldGrouping != null) {
            stream = GroupingHelper.group(stream, fieldGrouping, FieldEvaluator.forConnection(environment));
        }

        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return PaginationHelper.paginate(stream, n -> PaginationHelper.encodeCursor(n.getName()), arguments);
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Name")
    String getName();

    @GraphQLField
    @GraphQLDescription("Full display name")
    String getDisplayName();

    @GraphQLField
    @GraphQLDescription("Site where the principal is defined")
    GqlJcrSite getSite() throws RepositoryException;

    @GraphQLField
    @GraphQLDescription("Is this principal member of the specified group")
    boolean isMemberOf(@GraphQLName("group") String group,
                       @GraphQLName("site") @GraphQLDescription("Site where the group is defined") String site);

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("List of groups this principal belongs to")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    DXPaginatedData<GqlGroup> getGroupMembership(@GraphQLName("site") @GraphQLDescription("Return only groups which belong to this site") String site,
                                                 @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                 @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                 @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                 DataFetchingEnvironment environment);

    @GraphQLField
    @GraphQLDescription("Get the corresponding JCR node")
    GqlJcrNode getNode() throws RepositoryException;

}
