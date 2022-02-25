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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.predicate.*;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.utils.LanguageCodeConverters;
import pl.touk.throwing.ThrowingFunction;
import pl.touk.throwing.ThrowingPredicate;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NodeHelper {

    private static HashMap<GqlJcrNode.PropertyEvaluation, PropertyEvaluationAlgorithm> ALGORITHM_BY_EVALUATION = new HashMap<>();

    static {

        ALGORITHM_BY_EVALUATION.put(GqlJcrNode.PropertyEvaluation.PRESENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                return hasProperty(node, language, propertyName);
            }
        });

        ALGORITHM_BY_EVALUATION.put(GqlJcrNode.PropertyEvaluation.ABSENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                return !hasProperty(node, language, propertyName);
            }
        });

        ALGORITHM_BY_EVALUATION.put(GqlJcrNode.PropertyEvaluation.EQUAL, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                if (propertyValue == null) {
                    throw new GqlJcrWrongInputException("Property value is required for " + GqlJcrNode.PropertyEvaluation.EQUAL + " evaluation");
                }
                return hasPropertyValue(node, language, propertyName, propertyValue);
            }
        });

        ALGORITHM_BY_EVALUATION.put(GqlJcrNode.PropertyEvaluation.DIFFERENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                if (propertyValue == null) {
                    throw new GqlJcrWrongInputException("Property value is required for " + GqlJcrNode.PropertyEvaluation.DIFFERENT + " evaluation");
                }
                return !hasPropertyValue(node, language, propertyName, propertyValue);
            }
        });
    }

    public static Predicate<JCRNodeWrapper> getPropertiesPredicate(GqlJcrNode.NodePropertiesInput propertiesFilter) {
        Predicate<JCRNodeWrapper> propertiesPredicate;
        if (propertiesFilter == null) {
            propertiesPredicate = PredicateHelper.truePredicate();
        } else {
            LinkedList<Predicate<JCRNodeWrapper>> propertyPredicates = new LinkedList<>();
            for (GqlJcrNode.NodePropertyInput propertyFilter : propertiesFilter.getPropertyFilters()) {
                GqlJcrNode.PropertyEvaluation propertyEvaluation = propertyFilter.getPropertyEvaluation();
                if (propertyEvaluation == null) {
                    propertyEvaluation = GqlJcrNode.PropertyEvaluation.EQUAL;
                }
                PropertyEvaluationAlgorithm evaluationAlgorithm = ALGORITHM_BY_EVALUATION.get(propertyEvaluation);
                if (evaluationAlgorithm == null) {
                    throw new IllegalArgumentException("Unknown property evaluation: " + propertyEvaluation);
                }
                propertyPredicates.add(node -> evaluationAlgorithm.evaluate(node, propertyFilter.getLanguage(), propertyFilter.getPropertyName(), propertyFilter.getPropertyValue()));
            }
            propertiesPredicate = PredicateHelper.getCombinedPredicate(propertyPredicates, propertiesFilter.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ALL);
        }
        return propertiesPredicate;
    }

    public static Predicate<JCRNodeWrapper> getTypesPredicate(GqlJcrNode.NodeTypesInput typesFilter) {
        Predicate<JCRNodeWrapper> typesPredicate;
        if (typesFilter == null) {
            typesPredicate = PredicateHelper.truePredicate();
        } else {
            LinkedList<Predicate<JCRNodeWrapper>> typePredicates = new LinkedList<>();
            for (String typeFilter : typesFilter.getTypes()) {
                typePredicates.add(ThrowingPredicate.unchecked(node -> node.isNodeType(typeFilter)));
            }
            typesPredicate = PredicateHelper.getCombinedPredicate(typePredicates, typesFilter.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ANY);
        }
        return typesPredicate;
    }

    private static boolean hasProperty(JCRNodeWrapper node, String language, String propertyName) {
        try {
            node = getNodeInLanguage(node, language);
            return node.hasProperty(propertyName);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasPropertyValue(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
        try {
            node = getNodeInLanguage(node, language);
            if (!node.hasProperty(propertyName)) {
                return false;
            }
            final JCRPropertyWrapper property = node.getProperty(propertyName);
            if (!property.isMultiple())
                return (property.getString().equals(propertyValue));
            for (JCRValueWrapper value : property.getValues()) {
                if (value.getString().equals(propertyValue)) {
                    return true;
                }
            }
            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the JCR node using localized session. If <code>language</code> is <code>null</code>, returns the node itself.
     *
     * @param node the node to be retrieved in a localized session
     * @param language the language of the localized session to use
     * @return the JCR node using localized session. If <code>language</code> is <code>null</code>, returns the node itself
     * @throws RepositoryException in case of JCR access errors
     */
    public static JCRNodeWrapper getNodeInLanguage(JCRNodeWrapper node, String language) throws RepositoryException {
        if (language == null) {
            return node;
        }
        String workspace = node.getSession().getWorkspace().getName();
        Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace, locale);
        return session.getNodeByIdentifier(node.getIdentifier());
    }

    static void collectDescendants(JCRNodeWrapper node, Predicate<JCRNodeWrapper> predicate, Predicate<JCRNodeWrapper> recursionPredicate, Consumer<JCRNodeWrapper> consumer) throws RepositoryException {
        for (JCRNodeWrapper child : node.getNodes()) {
            if (predicate.test(child)) {
                consumer.accept(child);
            }
            if (recursionPredicate.test(child)) {
                collectDescendants(child, predicate, recursionPredicate, consumer);
            }
        }
    }

    static Predicate<JCRNodeWrapper> getNodesPredicate(final Collection<String> names, final GqlJcrNode.NodeTypesInput typesFilter, final GqlJcrNode.NodePropertiesInput propertiesFilter, DataFetchingEnvironment environment) {

        Predicate<JCRNodeWrapper> namesPredicate;
        if (names == null) {
            namesPredicate = PredicateHelper.truePredicate();
        } else {
            namesPredicate = (node) -> names.contains(node.getName());
        }

        Predicate<JCRNodeWrapper> typesPredicate = getTypesPredicate(typesFilter);
        Predicate<JCRNodeWrapper> propertiesPredicate = getPropertiesPredicate(propertiesFilter);

        Predicate<JCRNodeWrapper> permissionPredicate;
        if (environment == null) {
            permissionPredicate = PredicateHelper.truePredicate();
        } else {
            permissionPredicate = (node) -> PermissionHelper.hasPermission(node, environment);
        }

        Predicate<JCRNodeWrapper> result = PredicateHelper.allPredicates(Arrays.asList(GqlJcrNodeImpl.DEFAULT_CHILDREN_PREDICATE, namesPredicate, typesPredicate, propertiesPredicate, permissionPredicate));
        return result;
    }

    public static DXPaginatedData<GqlJcrNode> getPaginatedNodesList(NodeIterator it, Collection<String> names, GqlJcrNode.NodeTypesInput typesFilter, GqlJcrNode.NodePropertiesInput propertiesFilter, FieldFiltersInput fieldFilter,
            DataFetchingEnvironment environment, FieldSorterInput fieldSorterInput, FieldGroupingInput fieldGroupingInput) {

        @SuppressWarnings("unchecked") Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>)it, Spliterator.ORDERED), false)
            .filter(node-> PermissionHelper.hasPermission(node, environment))
            .filter(getNodesPredicate(names, typesFilter, propertiesFilter, environment))
            .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode))
            .filter(FilterHelper.getFieldPredicate(fieldFilter, FieldEvaluator.forConnection(environment)));

        if (fieldSorterInput != null) {
            stream = stream.sorted(SorterHelper.getFieldComparator(fieldSorterInput, FieldEvaluator.forConnection(environment)));
        }

        if (fieldGroupingInput != null) {
            stream = GroupingHelper.group(stream, fieldGroupingInput, FieldEvaluator.forConnection(environment));
        }

        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return PaginationHelper.paginate(stream, n -> {
            return PaginationHelper.encodeCursor(n.getUuid());
        }, arguments);
    }

    private interface PropertyEvaluationAlgorithm {
        boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue);
    }
}
