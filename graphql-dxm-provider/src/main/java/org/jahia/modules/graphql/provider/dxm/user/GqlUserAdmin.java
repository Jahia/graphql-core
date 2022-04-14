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
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUserManagerService;

import javax.inject.Inject;
import java.util.stream.Stream;

@GraphQLName("UserAdminQuery")
@GraphQLDescription("User admin queries")
public class GqlUserAdmin {

    @Inject
    @GraphQLOsgiService
    private JahiaUserManagerService userManagerService;

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
    @GraphQLDescription("Get users list")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlUser> getUsers(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
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
