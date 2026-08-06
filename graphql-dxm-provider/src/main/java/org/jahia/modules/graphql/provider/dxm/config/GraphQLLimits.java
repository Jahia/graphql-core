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
package org.jahia.modules.graphql.provider.dxm.config;

import org.jahia.modules.graphql.provider.dxm.GqlLimitExceededException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds the effective bound on how many nodes a single mutation field may operate on, and the rules for applying it.
 * <p>
 * This is deliberately separate from the pagination node limit ({@code graphql.fields.node.limit}, held by
 * {@code PaginationHelper}). The two bound comparable-looking numbers but very different work: the pagination limit
 * caps how many nodes a read field collects and serializes, whereas this one caps how many nodes a mutation field
 * takes handles for — and those handles accumulate pending changes in a single JCR session that is written in one
 * commit. Sizing the write batch therefore needs its own dial, so an operator can tighten mutations without
 * shrinking read pages (or the reverse).
 * <p>
 * The value is pushed in by {@link DXGraphQLConfig} whenever the configuration is recomputed, mirroring how the
 * pagination limit is maintained. A configured value of {@code 0} disables the bound.
 */
public final class GraphQLLimits {

    /**
     * Default maximum number of nodes a single mutation field may operate on.
     * <p>
     * NOTE: this initial value intentionally matches the pagination node limit, so that introducing this dial is not
     * itself a change of behaviour. It is a starting point rather than a considered figure — a write batch is more
     * expensive per node than a read page, so this is expected to be lowered once there is data on real batch sizes.
     */
    static final int DEFAULT_MUTATION_BATCH_LIMIT = 5000;

    private static final AtomicInteger mutationBatchLimit = new AtomicInteger(DEFAULT_MUTATION_BATCH_LIMIT);

    private GraphQLLimits() {
        throw new IllegalStateException("Utility class is not meant to be instantiated");
    }

    /**
     * Resolves the maximum number of nodes a mutation may operate on when the caller can express a limit itself, i.e.
     * for a query-driven mutation. An explicit limit is honoured but may never raise the result set above the
     * configured bound — a request argument must not be able to widen a global limit.
     *
     * @param requestedLimit the limit requested by the caller, may be {@code null}
     * @return the limit to apply, or {@code null} when no bound applies
     */
    public static Long resolveMutationLimit(Long requestedLimit) {
        long configuredLimit = mutationBatchLimit.get();
        Long requested = (requestedLimit != null && requestedLimit.longValue() > 0) ? requestedLimit : null;
        if (configuredLimit <= 0) {
            return requested;
        }
        return (requested == null) ? Long.valueOf(configuredLimit) : Long.valueOf(Math.min(requested.longValue(), configuredLimit));
    }

    /**
     * Verifies that a caller-supplied batch is within the configured bound.
     * <p>
     * A query-driven mutation never enumerated its targets, so returning fewer of them is a reasonable bound. Here the
     * caller listed the nodes explicitly, so silently operating on a prefix of that list would quietly not do what was
     * asked — a correctness problem dressed up as a limit. Failing tells the caller to split the batch instead.
     *
     * @param batchSize the number of entries the caller supplied
     * @throws GqlLimitExceededException if the batch exceeds the configured bound
     */
    public static void checkMutationBatchSize(int batchSize) {
        int configuredLimit = mutationBatchLimit.get();
        if (configuredLimit > 0 && batchSize > configuredLimit) {
            throw new GqlLimitExceededException("This mutation was given " + batchSize
                    + " nodes to operate on, which is more than the maximum of " + configuredLimit
                    + "; split the work into smaller batches.");
        }
    }

    /**
     * Key under which the pre-execution guard leaves what is left of a request's batch allowance in the GraphQL context,
     * for the fields whose cardinality it could not measure statically.
     */
    public static final String REMAINING_BATCH_ALLOWANCE = "jahiaRemainingMutationBatchAllowance";

    /**
     * @return the configured maximum number of nodes a single mutation field may operate on; {@code 0} means unbounded
     */
    public static int getMutationBatchLimit() {
        return mutationBatchLimit.get();
    }

    /**
     * Resolves how many nodes a query-driven mutation may operate on. The pre-execution guard cannot measure it - how
     * many nodes a JCR statement matches is only known once it runs - so the field checks the result against this and
     * fails if more matched. It draws from what the request has left rather than from the whole allowance, so several
     * aliased calls share one budget.
     *
     * @param requestedLimit the limit requested by the caller, may be {@code null}
     * @param remaining      the request's remaining allowance, or {@code null} when the guard is not in play
     * @return the limit to apply to the query, or {@code null} when no bound applies
     */
    public static Long resolveMutationLimit(Long requestedLimit, AtomicInteger remaining) {
        if (remaining == null) {
            return resolveMutationLimit(requestedLimit);
        }
        long left = Math.max(0, remaining.get());
        Long asked = (requestedLimit != null && requestedLimit.longValue() > 0) ? requestedLimit : null;
        return (asked == null) ? Long.valueOf(left) : Long.valueOf(Math.min(asked.longValue(), left));
    }

    /**
     * Updates the effective mutation batch limit. Called by {@link DXGraphQLConfig} when configuration changes.
     *
     * @param limit the new limit; {@code 0} disables the bound
     */
    public static void updateMutationBatchLimit(int limit) {
        mutationBatchLimit.set(limit);
    }
}
