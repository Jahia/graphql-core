package org.jahia.modules.graphql.provider.dxm.node;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.functors.AllPredicate;
import org.jahia.services.content.JCRItemWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;

import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;

@GraphQLName("GenericJCRNode")
public class GqlJcrNodeImpl implements GqlJcrNode {

    private JCRNodeWrapper node;
    private String type;

    public GqlJcrNodeImpl(JCRNodeWrapper node) {
        this(node, null);
    }

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
            JCRNodeWrapper node = this.node;
            if (language != null) {
                node = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(node.getIdentifier());
            }
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
        List<GqlJcrProperty> propertyList = new ArrayList<GqlJcrProperty>();
        try {
            JCRNodeWrapper node = this.node;
            if (language != null) {
                node = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(node.getIdentifier());
            }
            if (names != null && !names.isEmpty()) {
                for (String name : names) {
                    if (node.hasProperty(name)) {
                        propertyList.add(new GqlJcrProperty(node.getProperty(name)));
                    }
                }
            } else {
                PropertyIterator pi = node.getProperties();
                while (pi.hasNext()) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) pi.nextProperty();
                    propertyList.add(new GqlJcrProperty(property));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return propertyList;
    }

    @Override
    public GqlJcrProperty getProperty(@GraphQLName("name") String name,
                                      @GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = this.node;
            if (language != null) {
                node = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(node.getIdentifier());
            }
            if (node.hasProperty(name)) {
                return new GqlJcrProperty(node.getProperty(name));
            }
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLNonNull
    public List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                        @GraphQLName("anyType") Collection<String> anyType,
                                        @GraphQLName("properties") Collection<PropertyFilterTypeInput> properties,
                                        @GraphQLName("asMixin") String asMixin) {
        List<GqlJcrNode> children = new ArrayList<GqlJcrNode>();
        try {
            Iterator<JCRNodeWrapper> nodes = IteratorUtils.filteredIterator(node.getNodes().iterator(), getNodesPredicate(names,anyType,properties));
            while (nodes.hasNext()) {
                children.add(SpecializedTypesHandler.getNode(nodes.next()));
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    // List of input objects is not correctly handled by graphql-java-annotations, to fix
    private AllPredicate<JCRNodeWrapper> getNodesPredicate(final Collection<String> names, final Collection<String> anyType, final Collection<PropertyFilterTypeInput> properties) {

        return new AllPredicate<JCRNodeWrapper>(

                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {

                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        return names == null || names.isEmpty() || names.contains(node.getName());
                    }
                },

                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {

                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        if (anyType == null || anyType.isEmpty()) {
                            return true;
                        }
                        for (String type : anyType) {
                            try {
                                if (node.isNodeType(type)) {
                                    return true;
                                }
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return false;
                    }
                },

                new org.apache.commons.collections4.Predicate<JCRNodeWrapper>() {

                    @Override
                    public boolean evaluate(JCRNodeWrapper node) {
                        if (properties == null || properties.isEmpty()) {
                            return true;
                        }
                        for (PropertyFilterTypeInput property : properties) {
                            try {
                                if (!node.hasProperty(property.key) || !node.getProperty(property.key).getString().equals(property.value)) {
                                    return false;
                                }
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return true;
                    }
                }
        );
    }

    @Override
    @GraphQLNonNull
    public List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath) {
        List<GqlJcrNode> ancestors = new ArrayList<GqlJcrNode>();
        String upToPathSlash = upToPath + "/";
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
}
