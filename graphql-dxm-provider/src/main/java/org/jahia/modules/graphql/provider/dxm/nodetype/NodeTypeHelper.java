/**
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

package org.jahia.modules.graphql.provider.dxm.nodetype;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.predicate.PredicateHelper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.sites.JahiaSitesService;

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

    private static final Predicate<ExtendedNodeType> PREDICATE_IS_ABSTARCT = (nt) -> nt.isAbstract();

    private static final Predicate<ExtendedNodeType> PREDICATE_IS_MIXIN = (nt) -> nt.isMixin();

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
        if (input.getExcludedTypes() != null && !input.getExcludedTypes().isEmpty()) {
            // do not include excluded types
            predicates.add(
                    getTypesPredicate(new HashSet<>(input.getExcludedTypes()), input.getConsiderSubTypes()).negate());
        }
        if (input.getIncludedTypes() != null && !input.getIncludedTypes().isEmpty()) {
            // include specified types
            predicates.add(getTypesPredicate(new HashSet<>(input.getIncludedTypes()), input.getConsiderSubTypes()));
        }

        return predicates.isEmpty() ? PredicateHelper.truePredicate() : PredicateHelper.allPredicates(predicates);
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

    private static Stream<ExtendedNodeType> getStream(NodeTypesListInput input) throws RepositoryException {
        if (input != null && CollectionUtils.isNotEmpty(input.getModules())
                && StringUtils.isNotEmpty(input.getSiteKey())) {
            throw new IllegalArgumentException(
                    "Either a siteKey or a list of modules can be specified as filter, but not both");
        }
        List<String> modules = null;
        NodeTypeRegistry registry = NodeTypeRegistry.getInstance();
        if (input != null) {
            if (CollectionUtils.isNotEmpty(input.getModules())) {
                modules = input.getModules();
            }
            if (StringUtils.isNotEmpty(input.getSiteKey())) {
                modules = new LinkedList<>(
                        StringUtils.isNotEmpty(input.getSiteKey()) ? getModulesForSite(input.getSiteKey())
                                : input.getModules());
                modules.add("system-jahia");
            }
        }
        NodeTypeRegistry.JahiaNodeTypeIterator it = (modules != null ? registry.getAllNodeTypes(modules) : registry.getAllNodeTypes());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<ExtendedNodeType>)it, Spliterator.ORDERED), false);
    }

    /**
     * Returns a predicate that allows only specified node types, also considering their sub-types if <code>considerSubTypes</code> is
     * <code>true</code>.
     * 
     * @param types the list of types, the predicate will allow
     * @param considerSubTypes if <code>true</code>, we also allow sub-types of the specified node types
     * @return predicate that allows only specified node types, also considering their sub-types if <code>considerSubTypes</code> is
     *         <code>true</code>
     */
    private static Predicate<ExtendedNodeType> getTypesPredicate(Set<String> types, boolean considerSubTypes) {
        return types == null || types.isEmpty() ? PredicateHelper.truePredicate()
                : nt -> isNodeType(nt, types, considerSubTypes);
    }

    private static boolean isNodeType(ExtendedNodeType nt, Set<String> types, boolean considerSubTypes) {
        if (!considerSubTypes) {
            return types.contains(nt.getName());
        }
        for (String refType : types) {
            if (nt.isNodeType(refType)) {
                return true;
            }
        }
        return false;
    }

    private NodeTypeHelper() {
        super();
    }
}
