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
package org.jahia.modules.graphql.provider.dxm.nodetype;

import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.predicate.MulticriteriaEvaluation;
import org.jahia.modules.graphql.provider.dxm.predicate.PredicateHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.sites.JahiaSitesService;
import pl.touk.throwing.ThrowingPredicate;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for retrieving and filtering node types.
 *
 * @author Sergiy Shyrkov
 */
public final class NodeTypeHelper {

    private static final Predicate<ExtendedNodeType> PREDICATE_IS_ABSTARCT = (nodeType) -> nodeType.isAbstract();

    private static final Predicate<ExtendedNodeType> PREDICATE_IS_MIXIN = (nodeType) -> nodeType.isMixin();

    private static Predicate<ExtendedNodeType> getFilterPredicate(NodeTypesListInput input) {

        if (input == null) {
            return PredicateHelper.truePredicate();
        }

        List<Predicate<ExtendedNodeType>> predicates = new LinkedList<>();
        if (!input.getIncludeAbstract()) {
            // do not include abstract types
            predicates.add(PREDICATE_IS_ABSTARCT.negate());
        }
        if (!input.getIncludeMixins()) {
            // do not include mixins
            predicates.add(PREDICATE_IS_MIXIN.negate());
        }
        if (!input.getIncludeNonMixins()) {
            // do not include any types except mixins
            predicates.add(PREDICATE_IS_MIXIN);
        }
        if (input.getExcludeTypes() != null && !input.getExcludeTypes().isEmpty()) {
            // do not include excluded types
            predicates.add(
                    getTypesPredicate(new HashSet<>(input.getExcludeTypes()), input.getConsiderSubTypes()).negate());
        }
        if (input.getIncludeTypes() != null && !input.getIncludeTypes().isEmpty()) {
            // include specified types
            predicates.add(getTypesPredicate(new HashSet<>(input.getIncludeTypes()), input.getConsiderSubTypes()));
        }

        return predicates.isEmpty() ? PredicateHelper.truePredicate() : PredicateHelper.allPredicates(predicates);
    }

    public static Predicate<ExtendedNodeType> getTypesPredicate(GqlJcrNode.NodeTypesInput typesFilter) {
        Predicate<ExtendedNodeType> typesPredicate;
        if (typesFilter == null) {
            typesPredicate = PredicateHelper.truePredicate();
        } else {
            LinkedList<Predicate<ExtendedNodeType>> typePredicates = new LinkedList<>();
            for (String typeFilter : typesFilter.getTypes()) {
                typePredicates.add(ThrowingPredicate.unchecked(extendedNodeType -> extendedNodeType.isNodeType(typeFilter)));
            }
            typesPredicate = PredicateHelper.getCombinedPredicate(typePredicates, typesFilter.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ANY);
        }
        return typesPredicate;
    }

    private static Set<String> getModulesForSite(String siteKey) throws RepositoryException {
        JCRSiteNode site = JahiaSitesService.getInstance().getSiteByKey(siteKey,
                JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE));
        return site.getInstalledModulesWithAllDependencies();
    }

    /**
     * Returns a stream of node types, matching the specified input criteria.
     *
     * @param input the input criteria
     * @return a stream of node types, matching the specified input criteria
     * @throws RepositoryException in case of an error retrieving node type information
     */
    public static Stream<ExtendedNodeType> getNodeTypes(NodeTypesListInput input) throws RepositoryException {
        return getStream(input).filter(getFilterPredicate(input));
    }

    @SuppressWarnings("unchecked")
    private static Stream<ExtendedNodeType> getStream(NodeTypesListInput input) throws RepositoryException {

        List<String> modules = null;
        if (input != null) {
            List<String> inputModules = input.getModules();
            String inputSiteKey = input.getSiteKey();
            if (inputModules != null && inputSiteKey != null) {
                throw new IllegalArgumentException("Either a site key or a list of modules can be specified, but not both");
            }
            if (inputModules != null) {
                modules = inputModules;
            } else if (inputSiteKey != null) {
                modules = new LinkedList<>(getModulesForSite(inputSiteKey));
                modules.add("system-jahia");
            }
        }

        NodeTypeRegistry registry = NodeTypeRegistry.getInstance();
        NodeTypeRegistry.JahiaNodeTypeIterator it = (modules != null ? registry.getAllNodeTypes(modules) : registry.getAllNodeTypes());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<ExtendedNodeType>) it, Spliterator.ORDERED), false);
    }

    private static Predicate<ExtendedNodeType> getTypesPredicate(Set<String> nodeTypes, boolean considerSubTypes) {
        return nodeTypes == null || nodeTypes.isEmpty() ? PredicateHelper.truePredicate()
                : nodeType -> isNodeType(nodeType, nodeTypes, considerSubTypes);
    }

    private static boolean isNodeType(ExtendedNodeType nodeType, Set<String> nodeTypes, boolean considerSubTypes) {
        if (!considerSubTypes) {
            return nodeTypes.contains(nodeType.getName());
        }
        for (String nt : nodeTypes) {
            if (nodeType.isNodeType(nt)) {
                return true;
            }
        }
        return false;
    }

    private NodeTypeHelper() {
    }
}
