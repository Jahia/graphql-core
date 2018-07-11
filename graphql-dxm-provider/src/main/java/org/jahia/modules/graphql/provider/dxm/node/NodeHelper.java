/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.predicate.MulticriteriaEvaluation;
import org.jahia.modules.graphql.provider.dxm.predicate.PredicateHelper;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
                typePredicates.add(node -> {
                    try {
                        return node.isNodeType(typeFilter);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                });
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
            return (node.getProperty(propertyName).getString().equals(propertyValue));
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

    private interface PropertyEvaluationAlgorithm {
        boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue);
    }
}
