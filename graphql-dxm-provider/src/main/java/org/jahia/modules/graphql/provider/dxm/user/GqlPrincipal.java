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
package org.jahia.modules.graphql.provider.dxm.user;

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
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
    boolean isMemberOf(@GraphQLName("group") @GraphQLDescription("Target group") String group,
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
