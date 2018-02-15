/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.predicate;

import java.util.Collection;
import java.util.function.Predicate;

public class PredicateHelper {

    public static <T> Predicate<T> truePredicate() {
        return (object) -> true;
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
