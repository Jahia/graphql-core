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
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.kickstart.servlet.context.DefaultGraphQLWebSocketContext;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.apache.shiro.session.InvalidSessionException;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.usermanager.JahiaUser;
import org.reactivestreams.Publisher;

import javax.servlet.http.HttpSession;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
@GraphQLDescription("A sub extension that gives access to the users")
public class UserSubscriptionExtension {

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private UserSubscriptionExtension() {
    }

    @GraphQLField
    @GraphQLDescription("Get the current user")
    public static Publisher<GqlCurrentUser> getCurrentUser(DataFetchingEnvironment environment) {
        final HttpSession session = (HttpSession) ((DefaultGraphQLWebSocketContext)environment.getContext()).getSession().getUserProperties().get(HttpSession.class.getName());
        return Flowable.create(obs -> {
            ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(() -> {
                try {
                    JahiaUser user = (JahiaUser) session.getAttribute(Constants.SESSION_USER);
                    if (user != null) {
                        obs.onNext(new GqlCurrentUser(user));
                    } else {
                        obs.onNext(new GqlCurrentUser(null));
                    }
                } catch (InvalidSessionException e) {
                    obs.onNext(new GqlCurrentUser(null));
                }
            }, 0, 1, TimeUnit.SECONDS);
            obs.setCancellable(() -> {
                f.cancel(false);
            });
        }, BackpressureStrategy.BUFFER);
    }
}
