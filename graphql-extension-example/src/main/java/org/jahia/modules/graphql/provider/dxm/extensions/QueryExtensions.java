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
package org.jahia.modules.graphql.provider.dxm.extensions;


import graphql.annotations.annotationTypes.GraphQLDescription;
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
