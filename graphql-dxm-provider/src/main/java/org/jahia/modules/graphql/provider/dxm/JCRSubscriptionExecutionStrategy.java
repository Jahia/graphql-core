/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.*;
import graphql.execution.reactive.CompletionStageMappingPublisher;
import graphql.language.Field;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertTrue;
import static java.util.Collections.singletonMap;

// This class is only needed to port fix from : https://github.com/graphql-java/graphql-java/issues/844
public class JCRSubscriptionExecutionStrategy extends ExecutionStrategy {

    public JCRSubscriptionExecutionStrategy() {
        super();
    }

    public JCRSubscriptionExecutionStrategy(DataFetcherExceptionHandler dataFetcherExceptionHandler) {
        super(dataFetcherExceptionHandler);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws NonNullableFieldWasNullException {

        CompletableFuture<Publisher<Object>> sourceEventStream = createSourceEventStream(executionContext, parameters);

        //
        // when the upstream source event stream completes, subscribe to it and wire in our adapter
        return sourceEventStream.thenApply((publisher) -> {
            if (publisher == null) {
                return new ExecutionResultImpl(null, executionContext.getErrors());
            }
            CompletionStageMappingPublisher<ExecutionResult, Object> mapSourceToResponse = new CompletionStageMappingPublisher<>(
                    publisher,
                    eventPayload -> executeSubscriptionEvent(executionContext, parameters, eventPayload)
            );
            return new ExecutionResultImpl(mapSourceToResponse, executionContext.getErrors());
        });
    }

    /*
        https://github.com/facebook/graphql/blob/master/spec/Section%206%20--%20Execution.md

        CreateSourceEventStream(subscription, schema, variableValues, initialValue):

            Let {subscriptionType} be the root Subscription type in {schema}.
            Assert: {subscriptionType} is an Object type.
            Let {selectionSet} be the top level Selection Set in {subscription}.
            Let {rootField} be the first top level field in {selectionSet}.
            Let {argumentValues} be the result of {CoerceArgumentValues(subscriptionType, rootField, variableValues)}.
            Let {fieldStream} be the result of running {ResolveFieldEventStream(subscriptionType, initialValue, rootField, argumentValues)}.
            Return {fieldStream}.
     */

    private CompletableFuture<Publisher<Object>> createSourceEventStream(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        ExecutionStrategyParameters newParameters = firstFieldOfSubscriptionSelection(parameters);

        CompletableFuture<Object> fieldFetched = fetchField(executionContext, newParameters);
        return fieldFetched.thenApply(publisher -> {
            if (publisher != null) {
                assertTrue(publisher instanceof Publisher, "You data fetcher must return a Publisher of events when using graphql subscriptions");
            }
            //noinspection unchecked
            return (Publisher<Object>) publisher;
        });
    }

    /*
        ExecuteSubscriptionEvent(subscription, schema, variableValues, initialValue):

        Let {subscriptionType} be the root Subscription type in {schema}.
        Assert: {subscriptionType} is an Object type.
        Let {selectionSet} be the top level Selection Set in {subscription}.
        Let {data} be the result of running {ExecuteSelectionSet(selectionSet, subscriptionType, initialValue, variableValues)} normally (allowing parallelization).
        Let {errors} be any field errors produced while executing the selection set.
        Return an unordered map containing {data} and {errors}.

        Note: The {ExecuteSubscriptionEvent()} algorithm is intentionally similar to {ExecuteQuery()} since this is how each event result is produced.
     */

    private CompletableFuture<ExecutionResult> executeSubscriptionEvent(ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object eventPayload) {
        ExecutionContext newExecutionContext = executionContext.transform(builder -> builder.root(eventPayload));

        ExecutionStrategyParameters newParameters = firstFieldOfSubscriptionSelection(parameters);

        return completeField(newExecutionContext, newParameters, eventPayload)
                .thenApply(executionResult -> wrapWithRootFieldName(newParameters, executionResult));
    }

    private ExecutionResult wrapWithRootFieldName(ExecutionStrategyParameters parameters, ExecutionResult executionResult) {
        String rootFieldName = getRootFieldName(parameters);
        return new ExecutionResultImpl(
                singletonMap(rootFieldName, executionResult.getData()),
                executionResult.getErrors()
        );
    }

    private String getRootFieldName(ExecutionStrategyParameters parameters) {
        Field rootField = parameters.field().get(0);
        return rootField.getAlias() != null ? rootField.getAlias() : rootField.getName();
    }

    private ExecutionStrategyParameters firstFieldOfSubscriptionSelection(ExecutionStrategyParameters parameters) {
        Map<String, List<Field>> fields = parameters.fields();
        List<String> fieldNames = new ArrayList<>(fields.keySet());

        List<Field> firstField = fields.get(fieldNames.get(0));

        ExecutionPath fieldPath = parameters.path().segment(firstField.get(0).getName());
        return parameters.transform(builder -> builder.field(firstField).path(fieldPath));
    }
}