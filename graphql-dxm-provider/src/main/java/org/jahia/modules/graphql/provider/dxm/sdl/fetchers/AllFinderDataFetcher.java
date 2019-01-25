/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

public class AllFinderDataFetcher extends FinderDataFetcher {

    private static final String SORT_BY = "sortBy";
    private static final String FIELD_NAME = "fieldName";
    private static final String SORT_ORDER = "sortOrder";
    private static final String IGNORE_CASE = "ingoreCase";

    public AllFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLInputObjectField> sortFilterInputFields = new ArrayList<>();

        sortFilterInputFields.add(new GraphQLInputObjectField(FIELD_NAME, GraphQLString));
        sortFilterInputFields.add(new GraphQLInputObjectField(SORT_ORDER, new GraphQLEnumType("SortOrder", "sort order",
                Arrays.asList(new GraphQLEnumValueDefinition("ASC", "", SorterHelper.SortType.ASC),
                              new GraphQLEnumValueDefinition("DESC", "", SorterHelper.SortType.DESC)))));
        sortFilterInputFields.add(new GraphQLInputObjectField(IGNORE_CASE, GraphQLBoolean));

        GraphQLInputObjectType sortFitlerInputType = new GraphQLInputObjectType("SortFilter",
                "sort filters", sortFilterInputFields);
        List<GraphQLArgument> arguments = getDefaultArguments();
        arguments.add(GraphQLArgument
                .newArgument()
                .name(SORT_BY)
                .description("sort filter object")
                .type(sortFitlerInputType)
                .build());
        return arguments;
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment){
        Map sortByFitler = environment.getArgument(SORT_BY);
        FieldSorterInput sorterInput = null;
        if(sortByFitler!=null){
            sorterInput = new FieldSorterInput(
                    (String)sortByFitler.get(FIELD_NAME),
                    (SorterHelper.SortType) sortByFitler.get(SORT_ORDER),
                    (Boolean)sortByFitler.get(IGNORE_CASE)
            );
        }

        try {
            String statement = "select * from [\"" + type + "\"]";
            JCRNodeIteratorWrapper it = getCurrentUserSession(environment)
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(statement, Query.JCR_SQL2)
                    .execute()
                    .getNodes();

            Stream<GqlJcrNode> stream = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

            return sorterInput!=null ?
                    stream.sorted(SorterHelper.getFieldComparator(sorterInput, FieldEvaluator.forList(environment))).collect(Collectors.toList())
                    :
                    stream.collect(Collectors.toList());

        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }
}
