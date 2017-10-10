package org.jahia.modules.graphql.provider.dxm.node;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.functors.AllPredicate;
import org.apache.commons.collections4.functors.AnyPredicate;
import org.apache.commons.collections4.functors.TruePredicate;
import org.jahia.services.content.JCRItemWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;

import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * GraphQL representation of a JCR node - generic implementation.
 */
@GraphQLName("GenericJCRNode")
public class GqlJcrNodeImpl implements GqlJcrNode {

    private static HashMap<PropertyEvaluation, PropertyEvaluationAlgorithm> ALGORITHM_BY_EVALUATION = new HashMap<>();
    static {

        ALGORITHM_BY_EVALUATION.put(PropertyEvaluation.PRESENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                return hasProperty(node, language, propertyName);
            }
        });

        ALGORITHM_BY_EVALUATION.put(PropertyEvaluation.ABSENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                return !hasProperty(node, language, propertyName);
            }
        });

        ALGORITHM_BY_EVALUATION.put(PropertyEvaluation.EQUAL, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                if (propertyValue == null) {
                    throw new GqlJcrWrongInputException("Property value is required for " + PropertyEvaluation.EQUAL + " evaluation");
                }
                return hasPropertyValue(node, language, propertyName, propertyValue);
            }
        });

        ALGORITHM_BY_EVALUATION.put(PropertyEvaluation.DIFFERENT, new PropertyEvaluationAlgorithm() {

            @Override
            public boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue) {
                if (propertyValue == null) {
                    throw new GqlJcrWrongInputException("Property value is required for " + PropertyEvaluation.DIFFERENT + " evaluation");
                }
                return !hasPropertyValue(node, language, propertyName, propertyValue);
            }
        });
    }

    private JCRNodeWrapper node;
    private String type;

    /**
     * Create an instance that represents a JCR node to GraphQL.
     *
     * @param node The JCR node to represent
     */
    public GqlJcrNodeImpl(JCRNodeWrapper node) {
        this(node, null);
    }

    /**
     * Create an instance that represents a JCR node to GraphQL as a given node type.
     *
     * @param node The JCR node to represent
     * @param type The type name to represent the node as, or null to represent as node's primary type
     */
    public GqlJcrNodeImpl(JCRNodeWrapper node, String type) {
        this.node = node;
        if (type != null) {
            this.type = type;
        } else {
            try {
                this.type = node.getPrimaryNodeTypeName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public JCRNodeWrapper getNode() {
        return node;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    @GraphQLNonNull
    public String getUuid() {
        try {
            return node.getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public String getName() {
        return node.getName();
    }

    @Override
    @GraphQLNonNull
    public String getPath() {
        return node.getPath();
    }

    @Override
    public String getDisplayName(@GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = getNodeInLanguage(this.node, language);
            return node.getDisplayableName();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GqlJcrNode getParent() {
        try {
            return SpecializedTypesHandler.getNode(node.getParent());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public Collection<GqlJcrProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                                    @GraphQLName("language") String language) {
        List<GqlJcrProperty> properties = new ArrayList<GqlJcrProperty>();
        try {
            JCRNodeWrapper node = getNodeInLanguage(this.node, language);
            if (names != null) {
                for (String name : names) {
                    if (node.hasProperty(name)) {
                        properties.add(new GqlJcrProperty(node.getProperty(name), this));
                    }
                }
            } else {
                for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) it.nextProperty();
                    properties.add(new GqlJcrProperty(property, this));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    @Override
    public GqlJcrProperty getProperty(@GraphQLName("name") @GraphQLNonNull String name,
                                      @GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = getNodeInLanguage(this.node, language);
            if (!node.hasProperty(name)) {
                return null;
            }
            return new GqlJcrProperty(node.getProperty(name), this);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                        @GraphQLName("typesFilter") NodeTypesInput typesFilter,
                                        @GraphQLName("propertiesFilter") NodePropertiesInput propertiesFilter) {
        List<GqlJcrNode> children = new ArrayList<GqlJcrNode>();
        try {
            Iterator<JCRNodeWrapper> nodes = IteratorUtils.filteredIterator(node.getNodes().iterator(), getChildNodesPredicate(names, typesFilter, propertiesFilter));
            while (nodes.hasNext()) {
                children.add(SpecializedTypesHandler.getNode(nodes.next()));
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return children;
    }

    private Predicate<JCRNodeWrapper> getChildNodesPredicate(final Collection<String> names, final NodeTypesInput typesFilter, final NodePropertiesInput propertiesFilter) {

        Predicate<JCRNodeWrapper> namesPredicate;
        if (names == null) {
            namesPredicate = TruePredicate.truePredicate();
        } else {
            namesPredicate = new Predicate<JCRNodeWrapper>() {

                @Override
                public boolean evaluate(JCRNodeWrapper child) {
                    return names.contains(child.getName());
                }
            };
        }

        Predicate<JCRNodeWrapper> typesPredicate;
        if (typesFilter == null) {
            typesPredicate = TruePredicate.truePredicate();
        } else {
            LinkedList<Predicate<JCRNodeWrapper>> typePredicates = new LinkedList<>();
            for (String typeFilter : typesFilter.getTypes()) {
                typePredicates.add(new Predicate<JCRNodeWrapper>() {

                    @Override
                    public boolean evaluate(JCRNodeWrapper child) {
                        try {
                            return child.isNodeType(typeFilter);
                        } catch (RepositoryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            typesPredicate = getCombinedPredicate(typePredicates, typesFilter.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ANY);
        }

        Predicate<JCRNodeWrapper> propertiesPredicate;
        if (propertiesFilter == null) {
            propertiesPredicate = TruePredicate.truePredicate();
        } else {
            LinkedList<Predicate<JCRNodeWrapper>> propertyPredicates = new LinkedList<>();
            for (NodePropertyInput propertyFilter : propertiesFilter.getPropertyFilters()) {
                PropertyEvaluation propertyEvaluation = propertyFilter.getPropertyEvaluation();
                if (propertyEvaluation == null) {
                    propertyEvaluation = PropertyEvaluation.EQUAL;
                }
                PropertyEvaluationAlgorithm evaluationAlgorithm = ALGORITHM_BY_EVALUATION.get(propertyEvaluation);
                if (evaluationAlgorithm == null) {
                    throw new IllegalArgumentException("Unknown property evaluation: " + propertyEvaluation);
                }
                propertyPredicates.add(new Predicate<JCRNodeWrapper>() {

                    @Override
                    public boolean evaluate(JCRNodeWrapper child) {
                        return evaluationAlgorithm.evaluate(child, propertyFilter.getLanguage(), propertyFilter.getPropertyName(), propertyFilter.getPropertyValue());
                    }
                });
            }
            propertiesPredicate = getCombinedPredicate(propertyPredicates, propertiesFilter.getMulticriteriaEvaluation(), MulticriteriaEvaluation.ALL);
        }

        @SuppressWarnings("unchecked") Predicate<JCRNodeWrapper> result = AllPredicate.allPredicate(namesPredicate, typesPredicate, propertiesPredicate);
        return result;
    }

    @Override
    @GraphQLNonNull
    public List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath) {
        List<GqlJcrNode> ancestors = new ArrayList<GqlJcrNode>();

        String upToPathSlash;
        if (upToPath != null) {
            upToPathSlash = upToPath.endsWith("/") ? upToPath : upToPath + "/";
        } else {
            upToPathSlash = "/";
        }

        if (!node.getPath().startsWith(upToPathSlash)) {
            throw new IllegalArgumentException("Invalid parameter [upToPath]: " + upToPathSlash);
        }

        try {
            List<JCRItemWrapper> jcrAncestors = node.getAncestors();
            for (JCRItemWrapper ancestor : jcrAncestors) {
                if (upToPath == null || ancestor.getPath().equals(upToPath) || ancestor.getPath().startsWith(upToPathSlash)) {
                    ancestors.add(SpecializedTypesHandler.getNode((JCRNodeWrapper) ancestor));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return ancestors;
    }

    @Override
    @GraphQLNonNull
    public GqlJcrSite getSite() {
        try {
            return new GqlJcrSite(node.getResolveSite());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GqlJcrNode asMixin(@GraphQLName("type") String type) {
        try {
            if (!node.isNodeType(type)) {
                return null;
            }
            return SpecializedTypesHandler.getNode(node, type);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
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

    private static JCRNodeWrapper getNodeInLanguage(JCRNodeWrapper node, String language) throws RepositoryException {
        if (language == null) {
            return node;
        }
        String workspace = node.getSession().getWorkspace().getName();
        Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace, locale);
        return session.getNodeByIdentifier(node.getIdentifier());
    }

    private static <T> Predicate<T> getCombinedPredicate(Collection<Predicate<T>> predicates, MulticriteriaEvaluation multicriteriaEvaluation, MulticriteriaEvaluation defaultMulticriteriaEvaluation) {
        if (multicriteriaEvaluation == null) {
            multicriteriaEvaluation = defaultMulticriteriaEvaluation;
        }
        if (multicriteriaEvaluation == MulticriteriaEvaluation.ALL) {
            return AllPredicate.allPredicate(predicates);
        } else if (multicriteriaEvaluation == MulticriteriaEvaluation.ANY) {
            return AnyPredicate.anyPredicate(predicates);
        } else {
            throw new IllegalArgumentException("Unknown multicriteria evaluation: " + multicriteriaEvaluation);
        }
    }

    private interface PropertyEvaluationAlgorithm {

        boolean evaluate(JCRNodeWrapper node, String language, String propertyName, String propertyValue);
    }
}
