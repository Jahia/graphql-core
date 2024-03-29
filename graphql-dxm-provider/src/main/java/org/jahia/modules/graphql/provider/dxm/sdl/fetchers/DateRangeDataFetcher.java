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
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.modules.graphql.provider.dxm.sdl.validation.ArgumentValidator;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

/**
 * Created at Jan, 2019
 *
 * @author chooliyip
 **/
public class DateRangeDataFetcher extends FinderListDataFetcher {

    private static final String ARG_AFTER = "after";
    private static final String ARG_BEFORE = "before";
    private static final String ARG_LASTDAYS = "lastDays";

    public DateRangeDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> args = getDefaultArguments();
        args.add(GraphQLArgument.newArgument()
                .name(ARG_AFTER)
                .type(GraphQLString)
                .description("Select content after date")
                .build()
        );
        args.add(GraphQLArgument.newArgument()
                .name(ARG_BEFORE)
                .type(GraphQLString)
                .description("Select content before date")
                .build());
        args.add(GraphQLArgument.newArgument()
                .name(ARG_LASTDAYS)
                .type(GraphQLInt)
                .description("Select content within last days")
                .build()
        );
        return args;
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        if (!ArgumentValidator.validate(ArgumentValidator.ArgumentNames.DATE_RANGE, environment)
                || !ArgumentValidator.validate(ArgumentValidator.ArgumentNames.SORT_BY, environment)) {
            return Collections.emptyList();
        }

        return getStream(environment).collect(Collectors.toList());
    }

    @Override
    public Stream<GqlJcrNode> getStream(DataFetchingEnvironment environment) {
        if (!ArgumentValidator.validate(ArgumentValidator.ArgumentNames.DATE_RANGE, environment)
                || !ArgumentValidator.validate(ArgumentValidator.ArgumentNames.SORT_BY, environment)) {
            return Stream.empty();
        }

        try {
            String statement = this.buildSQL2Statement(environment);
            JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
            JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));
            return resolveCollection(stream, environment);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Construct SQL2 statement with arguments
     *
     * @param environment
     * @return
     */
    private String buildSQL2Statement(DataFetchingEnvironment environment) {
        String after = (String) SDLUtil.getArgument(ARG_AFTER, environment);
        String before = (String) SDLUtil.getArgument(ARG_BEFORE, environment);
        Integer lastDays = (Integer) SDLUtil.getArgument(ARG_LASTDAYS, environment);

        if (lastDays != null) {
            Date afterDate = DateUtils.addDays(new Date(), -lastDays);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
            after = dateFormat.format(afterDate);
        }

        SQL2DateTypeQuery sql2 = new SQL2DateTypeQuery();
        sql2.selectFrom(type).where().and(
                sql2.constrain(SQL2DateTypeQuery.OPERATOR_GTE, finder.getProperty(), sql2.castDate(after)),
                sql2.constrain(SQL2DateTypeQuery.OPERATOR_LTE, finder.getProperty(), sql2.castDate(before))
        );

        return sql2.getStatement();
    }

    private class SQL2DateTypeQuery {

        public static final String SPACE = " ";
        public static final String START_WITH_SPACE = " * ";
        public static final String OPERATOR_GTE = ">=";
        public static final String OPERATOR_LTE = "<=";

        final StringBuilder sb = new StringBuilder();

        SQL2DateTypeQuery() {
            //void
        }

        public SQL2DateTypeQuery selectFrom(String type) {
            sb.append("SELECT" + START_WITH_SPACE + "FROM" + " [\"" + type + "\"]");
            return this;
        }

        public SQL2DateTypeQuery where() {
            sb.append(SPACE);
            sb.append("WHERE");
            return this;
        }

        public SQL2DateTypeQuery and(String... constrains) {
            final List<String> trimedConstrains = Arrays.stream(constrains).filter(Objects::nonNull).collect(Collectors.toList());
            sb.append(SPACE);
            for (int i = 0; i < trimedConstrains.size(); i++) {
                if (!StringUtils.isBlank(trimedConstrains.get(i))) {
                    if (i > 0) sb.append(SPACE + "AND" + SPACE);
                    sb.append(trimedConstrains.get(i));
                }
            }
            return this;
        }

        public String constrain(String operator, String property, String value) {
            return StringUtils.isBlank(value) ? null : "[" + property + "]" + SPACE + operator + SPACE + value;
        }

        public String castDate(String value) {
            return StringUtils.isBlank(value) ? null : "CAST('" + value + "' AS DATE)";
        }

        public String getStatement() {
            return sb.toString();
        }
    }

}

