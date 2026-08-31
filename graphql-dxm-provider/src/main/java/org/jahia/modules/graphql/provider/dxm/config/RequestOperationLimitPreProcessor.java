package org.jahia.modules.graphql.provider.dxm.config;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounds how many operations one HTTP request may submit, and refuses a request that submits more.
 *
 * <p>The endpoint accepts a JSON array as a request body, each element of which is an independent operation with its
 * own document and variables. Every element is executed, concurrently, as part of the one request. So this bound is a
 * different measure from the ones the document analysis applies: those describe a single operation, whereas this
 * counts how many of them a request carries, and the two multiply. Every per-request budget - the node allowance,
 * the mutation batch allowance - is opened once per operation, and the complexity and depth ceilings are applied to
 * each operation on its own, which is what makes the count of operations the outer bound over all of them and worth
 * keeping the tightest.
 *
 * <p>A request over the bound is refused as a whole, with 400, rather than trimmed to the bound: the caller submitted
 * a set of operations and expects an answer for each, so answering a prefix of them would silently not do what was
 * asked. The response says how many were submitted and what the maximum is, so a client can split the work.
 *
 * <p>The value is pushed in by {@link DXGraphQLConfig} whenever the configuration is recomputed, mirroring how the
 * node and mutation batch limits are maintained, and is held here because this is where it is spent. A configured
 * value of {@code 0} disables the bound.
 */
public class RequestOperationLimitPreProcessor implements BatchInputPreProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RequestOperationLimitPreProcessor.class);

    /**
     * Default maximum number of operations one request may submit.
     *
     * <p>Chosen to leave room for the clients that batch legitimately - the widely used ones default to ten
     * operations per request - while keeping the factor by which a request can multiply the per-operation ceilings
     * small enough to reason about.
     *
     * <p>The same number appears in the shipped {@code org.jahia.modules.graphql.provider-default.cfg} and in the
     * {@code 04-setRequestOperationLimit.started.groovy} patch that writes the property into the configuration of an
     * already-installed instance. Changing it here means changing it in all three.
     */
    static final int DEFAULT_OPERATION_LIMIT = 20;

    private static final AtomicInteger operationLimit = new AtomicInteger(DEFAULT_OPERATION_LIMIT);

    /**
     * @return the configured maximum number of operations one request may submit; {@code 0} means unbounded
     */
    public static int getOperationLimit() {
        return operationLimit.get();
    }

    /**
     * Updates the effective operation limit. Called by {@link DXGraphQLConfig} when configuration changes.
     *
     * @param limit the new limit; {@code 0} disables the bound
     */
    public static void updateOperationLimit(int limit) {
        operationLimit.set(limit);
    }

    @Override
    public BatchInputPreProcessResult preProcessBatch(GraphQLBatchedInvocationInput batchedInvocationInput,
                                                     HttpServletRequest request, HttpServletResponse response) {
        int configuredLimit = operationLimit.get();
        int submitted = batchedInvocationInput.getInvocationInputs().size();
        if (configuredLimit > 0 && submitted > configuredLimit) {
            logger.warn("Refusing a request submitting {} operations, more than the maximum of {}.",
                    submitted, configuredLimit);
            return new BatchInputPreProcessResult(HttpServletResponse.SC_BAD_REQUEST,
                    "This request submitted " + submitted + " operations, which is more than the maximum of "
                            + configuredLimit + "; split the work into smaller requests.");
        }
        return new BatchInputPreProcessResult(batchedInvocationInput);
    }
}
