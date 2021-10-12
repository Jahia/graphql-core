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
package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLAsync;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.query.QueryWrapper;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class QueryExtensions {

    @GraphQLField
    @GraphQLDescription("Sample extension")
    public static String testExtension(@GraphQLName("arg") @GraphQLDescription("Sample extension argument") String arg) {
        return "test " + arg;
    }

    @GraphQLField
    @GraphQLDescription("Sample newsById query extension")
    public static GqlNews getNewsById(@GraphQLName("id") @GraphQLDescription("id argument") String id) throws RepositoryException {
        return new GqlNews(JCRSessionFactory.getInstance().getCurrentUserSession().getNodeByIdentifier(id));
    }

    @GraphQLField
    @GraphQLDescription("Sample newsByPath query extension")
    public static GqlNews getNewsByPath(@GraphQLName("path") @GraphQLDescription("path argument") String path) throws RepositoryException {
        return new GqlNews(JCRSessionFactory.getInstance().getCurrentUserSession().getNode(path));
    }

    @GraphQLField
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    @GraphQLDescription("Sample newsByDate query extension")
    public static DXPaginatedData<GqlNews> getNewsByDate(@GraphQLName("afterDate") @GraphQLDescription("Sample afterDate argument") String after, @GraphQLName("beforeDate") @GraphQLDescription("Sample beforeDate argument") String before, DataFetchingEnvironment environment) throws RepositoryException  {
        QueryWrapper query = JCRSessionFactory.getInstance().getCurrentUserSession().getWorkspace().getQueryManager().createQuery("select * from [jnt:news] where [date]>'"
                + ISO8601.format(ISO8601.parse(after))
                + "' and [date]<'"
                + ISO8601.format(ISO8601.parse(before))
                + "'", Query.JCR_SQL2);

        Stream<GqlNews> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>)query.execute().getNodes(), Spliterator.ORDERED), false).map(GqlNews::new);

        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);

        return PaginationHelper.paginate(stream, n -> PaginationHelper.encodeCursor(n.getUuid()), arguments);
    }

    @GraphQLField
    @GraphQLDescription("Sample extension long field")
    public static String longField(@GraphQLName("arg") @GraphQLDescription("Sample long extension argument") String arg) throws InterruptedException {
        Thread.sleep(500);
        return "test " + Thread.currentThread().getName() + " : " + arg;
    }

    @GraphQLField
    @GraphQLAsync
    @GraphQLDescription("Sample extension async long field")
    public static String asyncLongField(@GraphQLName("arg") @GraphQLDescription("Sample asyncLong extension argument") String arg) throws InterruptedException  {
        Thread.sleep(500);
        return "test " + Thread.currentThread().getName() + " : " + arg;
    }
}
