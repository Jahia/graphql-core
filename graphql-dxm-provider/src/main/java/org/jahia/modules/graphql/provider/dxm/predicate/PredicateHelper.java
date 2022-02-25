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
package org.jahia.modules.graphql.provider.dxm.predicate;

import java.util.Collection;
import java.util.function.Predicate;

public class PredicateHelper {

    public static <T> Predicate<T> truePredicate() {
        return (object) -> true;
    }

    public static <T> Predicate<T> falsePredicate() {
        return (object) -> false;
    }

    public static <T> Predicate<T> anyPredicate(Collection<Predicate<T>> predicates) {
        return predicates.stream().reduce(Predicate::or).orElse(t->false);
    }

    public static <T> Predicate<T> allPredicates(Collection<Predicate<T>> predicates) {
        return predicates.stream().reduce(Predicate::and).orElse(t->false);
    }

    /**
     * Combine multiple predicate based on MulticriteriaEvaluation value
     * @param predicates The list of predicates
     * @param multicriteriaEvaluation How to combine them
     * @param defaultMulticriteriaEvaluation Default combination
     * @param <T> The type of objects the predicate is testing
     * @return A combined predicate
     */
    public static <T> Predicate<T> getCombinedPredicate(Collection<Predicate<T>> predicates, MulticriteriaEvaluation multicriteriaEvaluation, MulticriteriaEvaluation defaultMulticriteriaEvaluation) {
        if (multicriteriaEvaluation == null) {
            multicriteriaEvaluation = defaultMulticriteriaEvaluation;
        }
        if (multicriteriaEvaluation == MulticriteriaEvaluation.ALL) {
            return allPredicates(predicates);
        } else if (multicriteriaEvaluation == MulticriteriaEvaluation.ANY) {
            return anyPredicate(predicates);
        } else if (multicriteriaEvaluation == MulticriteriaEvaluation.NONE) {
            return anyPredicate(predicates).negate();
        } else {
            throw new IllegalArgumentException("Unknown multicriteria evaluation: " + multicriteriaEvaluation);
        }
    }

}
