package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

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

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

/**
 * Created at Jan, 2019
 *
 * @author chooliyip
 **/
public class DateRangeDataFetcher extends FinderDataFetcher{

    private static final String ARG_AFTER = "after";
    private static final String ARG_BEFORE = "before";
    private static final String ARG_LASTDAYS = "lastDays";
    private static final String PREVIEW = "preview";
    private static final String LANGUAGE = "language";

    public DateRangeDataFetcher(Finder finder){
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> args = new ArrayList<>();
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
        args.add(GraphQLArgument.newArgument()
                .name(PREVIEW)
                .type(GraphQLBoolean)
                .description("Return content from live or default workspace")
                .defaultValue(false)
                .build());
        args.add(GraphQLArgument.newArgument()
                .name(LANGUAGE)
                .type(GraphQLString)
                .description("Content language, defaults to English")
                .defaultValue("en")
                .build());
        return args;
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        if(hasValidArguments(environment)){
            try {
                Map<String, Object> arguments = environment.getArguments();
                String statement = this.buildSQL2Statement(environment);

                boolean preview = (Boolean) arguments.get(PREVIEW);
                Locale locale = LanguageCodeConverters.languageCodeToLocale((String) arguments.get(LANGUAGE));
                JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession(preview ? Constants.EDIT_WORKSPACE : Constants.LIVE_WORKSPACE, locale);
                JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
                Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                        .filter(node -> PermissionHelper.hasPermission(node, environment))
                        .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

                return stream.collect(Collectors.toList());
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        }else{
            throw new DataFetchingException("By date range data fetcher needs at least one argument of 'after', 'before' or 'lastDays'");
        }

    }

    /**
     * Construct SQL2 statement with arguments
     *
     * @param environment
     * @return
     */
    private String buildSQL2Statement(DataFetchingEnvironment environment){
        String after = environment.getArgument(ARG_AFTER);
        String before = environment.getArgument(ARG_BEFORE);
        Integer lastDays = environment.getArgument(ARG_LASTDAYS);

        if(lastDays!=null){
            Date afterDate = DateUtils.addDays(new Date(), - lastDays);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
            after = dateFormat.format(afterDate);
        }

        SQL2DateTypeQuery sql2 = new SQL2DateTypeQuery();
        sql2.selectFrom(type).where().and(
                sql2.constrain(sql2.OPERATOR_GTE, finder.getProperty(), sql2.castDate(after)),
                sql2.constrain(sql2.OPERATOR_LTE, finder.getProperty(), sql2.castDate(before))
        );

        return sql2.getStatement();
    }

    /**
     * Either one of the argument of after or before is needed
     *
     * @param environment
     * @return
     */
    private boolean hasValidArguments(DataFetchingEnvironment environment){
        if(environment.getArguments().size() < 1){
            return false;
        }
        else if (environment.getArgument(ARG_LASTDAYS) !=null &&
                (!StringUtils.isBlank(environment.getArgument(ARG_AFTER)) || !StringUtils.isBlank(environment.getArgument(ARG_BEFORE)))){
            return false;
        }else{
            return true;
        }
    }

    private class SQL2DateTypeQuery {

        public String SPACE = " ";
        public String START_WITH_SPACE = " * ";
        public String OPERATOR_GTE = ">=";
        public String OPERATOR_LTE = "<=";

        final StringBuffer sb = new StringBuffer();

        SQL2DateTypeQuery(){
            //void
        }

        public SQL2DateTypeQuery selectFrom(String type){
            sb.append("SELECT" + START_WITH_SPACE + "FROM" + " [\"" + type + "\"]");
            return this;
        }

        public SQL2DateTypeQuery where(){
            sb.append(SPACE);
            sb.append("WHERE");
            return this;
        }

        public SQL2DateTypeQuery and(String... constrains){
            final List<String> trimedConstrains = Arrays.stream(constrains).filter(Objects::nonNull).collect(Collectors.toList());
            sb.append(SPACE);
            for (int i=0; i<trimedConstrains.size();i++){
                if(!StringUtils.isBlank(trimedConstrains.get(i))){
                    if(i>0) sb.append(SPACE + "AND" + SPACE);
                    sb.append(trimedConstrains.get(i));
                }
            }
            return this;
        }

        public String constrain(String operator, String property, String value){
            return StringUtils.isBlank(value) ? null : "[" + property + "]" + SPACE + operator + SPACE + value;
        }

        public String castDate(String value){
            return StringUtils.isBlank(value) ? null : "CAST('" + value + "' AS DATE)";
        }

        public String getStatement(){
            return sb.toString();
        }
    }

}

