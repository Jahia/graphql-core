/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import org.jahia.modules.graphql.provider.dxm.node.FieldGroupingInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import java.util.*;
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

    public static Stream<GqlJcrNode> group(Stream<GqlJcrNode> stream, FieldGroupingInput fieldGroupingInput, FieldEvaluator fieldEvaluator) {

        List<GqlJcrNode> originalList = stream.collect(Collectors.toList());
        //Create buckets for groups
        Map<String, List<GqlJcrNode>> buckets = new LinkedHashMap<>();
        buckets.put(UNGROUPED_LIST, new ArrayList<>());
        for (String group : fieldGroupingInput.getGroups()) {
            buckets.put(group, new ArrayList<>());
        }

        //Distribute nodes among groups
        for (GqlJcrNode node : originalList) {
            String groupByFieldValue = (String)fieldEvaluator.getFieldValue(node, fieldGroupingInput.getFieldName());
            if (buckets.containsKey(groupByFieldValue)) {
                buckets.get(groupByFieldValue).add(node);
            }
            else {
                buckets.get(UNGROUPED_LIST).add(node);
            }
        }

        List<GqlJcrNode> groupedList = new ArrayList<>();

        //Concat grouped lists together in order they were processed
        for(Map.Entry<String, List<GqlJcrNode>> entry : buckets.entrySet()) {
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
