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
package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Class with sorting algorithm
 *
 * @author ybenchadi
 */
public class SorterHelper {

    public enum SortType {

        /**
         * Ascendant order
         */
        @GraphQLDescription("Ascendant order")
        ASC,

        /**
         * Descendant order
         */
        @GraphQLDescription("Descendant order")
        DESC
    }

    private static final Map<SortType, FieldSorterAlgorithm> SORT_BY_DIRECTION = new EnumMap<>(SortType.class);

    @FunctionalInterface interface FieldSorterAlgorithm {
        int evaluate(Object source, String fieldName, Object fieldValue, boolean ignoreCase, FieldEvaluator environment);
    }

    static {
        SORT_BY_DIRECTION.put(SortType.ASC, ((source, fieldName, fieldValue, ignoreCase, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value == null && fieldValue == null) {
                return 0;
            } else if (value == null && fieldValue != null) {
                return -1;
            } else if (value != null && fieldValue == null) {
                return 1;
            } else if (fieldValue instanceof Boolean) {
                return Boolean.compare((Boolean)value, (Boolean)fieldValue);
            } else if (fieldValue instanceof Long) {
                return Long.compare((Long)value, (Long)fieldValue);
            } else if (fieldValue instanceof Double) {
                return Double.compare((Double)value, (Double)fieldValue);
            } else {
                return ignoreCase ? value.toString().compareToIgnoreCase((String)fieldValue) : value.toString().compareTo((String)fieldValue);
            }
        }));

        SORT_BY_DIRECTION.put(SortType.DESC, ((source, fieldName, fieldValue, ignoreCase, environment) -> {
            Object value = environment.getFieldValue(source, fieldName);
            if (value == null && fieldValue == null) {
                return 0;
            } else if (value == null && fieldValue != null) {
                return 1;
            } else if (value != null && fieldValue == null) {
                return -1;
            } else if (fieldValue instanceof Boolean) {
                return Boolean.compare((Boolean)fieldValue, (Boolean)value);
            } else if (fieldValue instanceof Long) {
                return Long.compare((Long)fieldValue, (Long)value);
            } else if (fieldValue instanceof Double) {
                return Double.compare((Double)fieldValue, (Double)value);
            } else {
                return ignoreCase ? -(value.toString().compareToIgnoreCase((String)fieldValue)) : -(value.toString().compareTo((String)fieldValue));
            }
        }));

    }

    public static Comparator<Object> getFieldComparator(FieldSorterInput sortFilter, FieldEvaluator environment) {
        SorterHelper.SortType sortType = sortFilter.getSortType();
        if (sortType == null) {
            sortType = SorterHelper.SortType.ASC;
        }
        SorterHelper.FieldSorterAlgorithm sorterAlgorithm = SORT_BY_DIRECTION.get(sortType);
        if (sorterAlgorithm == null) {
            throw new IllegalArgumentException("Unknown sort direction : " + sortType);
        }
        return ((object, obj) -> sorterAlgorithm.evaluate(object, sortFilter.getFieldName(), environment.getFieldValue(obj,
                sortFilter.getFieldName()), (sortFilter.isIgnoreCase()==null || sortFilter.isIgnoreCase()), environment));
    }
}
