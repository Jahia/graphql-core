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

import graphql.annotations.annotationTypes.GraphQLDescription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Grouping logic
 *
 * @author akarmanov
 */
public class GroupingHelper {

    private static final String UNGROUPED_LIST = "theRest";

    public enum GroupingType {

        @GraphQLDescription("Put grouped items at the end in the order groups appear in the 'groups' list")
        END,

        @GraphQLDescription("Put grouped items at the start in the order groups appear in the 'groups' list")
        START
    }

    public static <T> Stream<T> group(Stream<T> stream, FieldGroupingInput fieldGroupingInput, FieldEvaluator fieldEvaluator) {

        List<T> originalList = stream.collect(Collectors.toList());
        //Create buckets for groups
        Map<String, List<T>> buckets = new LinkedHashMap<>();
        buckets.put(UNGROUPED_LIST, new ArrayList<>());
        for (String group : fieldGroupingInput.getGroups()) {
            buckets.put(group, new ArrayList<>());
        }

        //Distribute nodes among groups
        for (T node : originalList) {
            String groupByFieldValue = (String)fieldEvaluator.getFieldValue(node, fieldGroupingInput.getFieldName());
            if (buckets.containsKey(groupByFieldValue)) {
                buckets.get(groupByFieldValue).add(node);
            }
            else {
                buckets.get(UNGROUPED_LIST).add(node);
            }
        }

        List<T> groupedList = new ArrayList<>();

        //Concat grouped lists together in order they were processed
        for(Map.Entry<String, List<T>> entry : buckets.entrySet()) {
            if (!entry.getKey().equals(UNGROUPED_LIST)) {
                groupedList.addAll(entry.getValue());
            }
        }

        //Return grouped nodes either at start or end or original order
        if (fieldGroupingInput.getGroupingType().equals(GroupingType.END)) {
            buckets.get(UNGROUPED_LIST).addAll(groupedList);
            return buckets.get(UNGROUPED_LIST).stream();
        }
        else if (fieldGroupingInput.getGroupingType().equals(GroupingType.START)) {
            groupedList.addAll(buckets.get(UNGROUPED_LIST));
            return groupedList.stream();
        }

        return originalList.stream();
    }
}
