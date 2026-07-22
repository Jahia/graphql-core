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

import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Request-scoped cache for resolved {@link GqlUser} objects.
 *
 * <p>A single GraphQL request may reference the same username many times across many JCR nodes
 * (e.g., 50 content rows all last-modified by the same user). Without caching, each reference
 * triggers a separate {@code JahiaUserManagerService.lookupUser()} call. This cache deduplicates
 * those calls to at most one {@code lookupUser} per unique {@code (siteKey, username)} pair within
 * a single GraphQL execution.
 *
 * <p>The cache is backed by a {@link ThreadLocal} and must be cleared at the end of each GraphQL
 * execution via {@link #clear()} — this is done by
 * {@code JCRInstrumentation.instrumentExecutionResult}.
 *
 * <p>{@code null} results (users that do not exist) are also cached as
 * {@link Optional#empty()} so that repeated lookups for missing users do not issue redundant
 * backend calls within the same request.
 *
 * <p>At the end of each request, an INFO log is emitted summarising the number of cache hits,
 * misses, and unique users resolved. This can be used to assess cache effectiveness in production.
 */
public final class UserResolutionCache {

    private static final Logger logger = LoggerFactory.getLogger(UserResolutionCache.class);

    private static final ThreadLocal<Map<String, Optional<GqlUser>>> CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private static final ThreadLocal<int[]> STATS =
            ThreadLocal.withInitial(() -> new int[2]); // [0]=hits, [1]=misses

    private UserResolutionCache() {
    }

    /**
     * Returns whether the cache contains an entry for {@code (username, siteKey)}.
     *
     * @param username the username to look up (never {@code null})
     * @param siteKey  the site key, or {@code null} for global users
     * @return {@code true} if a cache entry exists (even for a non-existent user)
     */
    private static boolean isCached(String username, String siteKey) {
        return CACHE.get().containsKey(cacheKey(username, siteKey));
    }

    /**
     * Stores a resolved user (or confirmed absence) in the request-scoped cache.
     *
     * @param username the username (never {@code null})
     * @param siteKey  the site key, or {@code null} for global users
     * @param user     the resolved user, or {@code null} if the user does not exist
     */
    public static void put(String username, String siteKey, GqlUser user) {
        CACHE.get().put(cacheKey(username, siteKey), Optional.ofNullable(user));
    }

    /**
     * Clears the request-scoped cache for the current thread and logs a cache effectiveness
     * summary at INFO level.
     * Must be called at the end of each GraphQL execution to prevent stale data leaking
     * into subsequent requests on the same thread.
     */
    public static void clear() {
        int[] stats = STATS.get();
        int hits = stats[0];
        int misses = stats[1];
        int uniqueUsers = CACHE.get().size();
        if (hits + misses > 0) {
            logger.info("UserResolutionCache request summary: {} unique user(s) resolved, {} cache hit(s), {} cache miss(es)",
                    uniqueUsers, hits, misses);
        }
        CACHE.remove();
        STATS.remove();
    }

    /**
     * Resolves a {@link GqlUser} for the given username, using the request-scoped cache to
     * avoid repeated {@code lookupUser} calls within the same GraphQL execution.
     *
     * @param username           the username to resolve; {@code null} or blank returns {@code null}
     * @param siteKey            the site key for site-scoped lookup, or {@code null} for global users
     * @param userManagerService the OSGi user manager service
     * @return the resolved {@link GqlUser}, or {@code null} if the user does not exist, or if the user manager service is {@code null}
     */
    public static GqlUser resolve(String username, String siteKey, JahiaUserManagerService userManagerService) {
        if (username == null || username.isEmpty() || userManagerService == null) {
            return null;
        }
        if (isCached(username, siteKey)) {
            STATS.get()[0]++;
            return CACHE.get().get(cacheKey(username, siteKey)).orElse(null);
        }
        STATS.get()[1]++;
        JCRUserNode userNode = userManagerService.lookupUser(username, siteKey);
        GqlUser result = userNode != null ? new GqlUser(userNode.getJahiaUser()) : null;
        put(username, siteKey, result);
        return result;
    }

    private static String cacheKey(String username, String siteKey) {
        return (siteKey != null ? siteKey : "") + "\0" + username;
    }
}
