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
 * Holds the effective bound on how many nodes one request may operate on, and the rules for applying it.
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
     * Verifies that a caller-supplied batch is within the configured bound.
     * <p>
     * A query-driven mutation given a {@code limit} states its own page size, and paging to it is what was asked;
     * given none, how many nodes it matches is known only as it runs, so it too fails rather than operate on a subset.
     * Here the caller listed the nodes explicitly, so silently operating on a prefix of that list would quietly not do
     * what was asked — a correctness problem dressed up as a limit. Failing tells the caller to split the batch
     * instead.
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
     * @return the configured maximum number of nodes one request may operate on; {@code 0} means unbounded
     */
    public static int getMutationBatchLimit() {
        return mutationBatchLimit.get();
    }

    /**
     * Resolves the bound on how many nodes a query-driven mutation may operate on. It draws from what the request has
     * left rather than from the whole allowance, so several aliased calls share one budget.
     * <p>
     * The caller's own {@code limit} is deliberately not a parameter: it is compared against the configured limit
     * rather than folded into this bound. That reasoning belongs with the comparison, in
     * {@code GqlJcrMutation.mutateNodesByQuery}.
     *
     * @param remaining the request's remaining allowance, or {@code null} when the guard is not in play
     * @return the number of nodes the mutation may operate on, or {@code null} when unbounded
     */
    public static Long resolveMutationBatchBound(AtomicInteger remaining) {
        if (remaining != null) {
            return Long.valueOf(Math.max(0, remaining.get()));
        }
        int configuredLimit = mutationBatchLimit.get();
        return (configuredLimit > 0) ? Long.valueOf(configuredLimit) : null;
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
