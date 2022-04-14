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
import org.jahia.data.viewhelper.principal.PrincipalViewHelper;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.site.GqlJcrSite;
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
import java.util.Objects;
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
    @GraphQLDescription("Site where the group is defined")
    public GqlJcrSite getSite() throws RepositoryException {
        return new GqlJcrSite(jcrSessionFactory.getCurrentUserSession().getNode(group.getLocalPath()).getResolveSite());
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Group members")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlPrincipal> getMembers(@GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                    @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                    @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                    DataFetchingEnvironment environment) {
        Stream<GqlPrincipal> stream = groupManagerService.lookupGroupByPath(group.getLocalPath()).getMembers().stream()
                .map(this::convertMember)
                .filter(Objects::nonNull)
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

    private GqlPrincipal convertMember(JCRNodeWrapper node) {
        if (node instanceof JCRUserNode) {
            return new GqlUser(((JCRUserNode) node).getJahiaUser());
        } else if (node instanceof JCRGroupNode) {
            return new GqlGroup(((JCRGroupNode) node).getJahiaGroup());
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Is this principal member of the specified group")
    public boolean isMemberOf(@GraphQLName("group") @GraphQLDescription("Target group") String group,
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
    public DXPaginatedData<GqlGroup> getGroupMembership(@GraphQLName("site") @GraphQLDescription("Return only groups which belong to this site") String site,
                                                        @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
                                                        @GraphQLName("fieldSorter") @GraphQLDescription("Sort by graphQL fields values") FieldSorterInput fieldSorter,
                                                        @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields according to specified criteria") FieldGroupingInput fieldGrouping,
                                                        DataFetchingEnvironment environment) {
        return GqlPrincipal.getGroupMembership(group.getLocalPath(), site, fieldFilter, fieldSorter, fieldGrouping, environment, groupManagerService);
    }

    @GraphQLField
    @GraphQLDescription("Get the corresponding JCR node")
    public GqlJcrNode getNode() throws RepositoryException {
        return SpecializedTypesHandler.getNode(jcrSessionFactory.getCurrentUserSession().getNode(group.getLocalPath()));
    }
}
