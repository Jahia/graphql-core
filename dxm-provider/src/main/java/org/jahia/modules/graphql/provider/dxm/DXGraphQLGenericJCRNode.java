package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.functors.AllPredicate;
import org.jahia.services.content.JCRItemWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

@GraphQLName("GenericJCRNode")
public class DXGraphQLGenericJCRNode implements DXGraphQLJCRNode {
    private JCRNodeWrapper node;
    private String type;

    public DXGraphQLGenericJCRNode(JCRNodeWrapper node) {
        this.node = node;
        try {
            this.type = node.getPrimaryNodeTypeName();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public DXGraphQLGenericJCRNode(JCRNodeWrapper node, String type) {
        this.node = node;
        if (type != null) {
            this.type = type;
        } else {
            try {
                this.type = node.getPrimaryNodeTypeName();
            } catch (RepositoryException e) {
                e.printStackTrace();
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
    public String getUuid() {
        try {
            return node.getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
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
    public DXGraphQLJCRNode getParent() {
        try {
            return new DXGraphQLGenericJCRNode(node.getParent());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DXGraphQLNodeType getPrimaryNodeType() {
        try {
            return new DXGraphQLNodeType(node.getPrimaryNodeType());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DXGraphQLNodeType> getMixinTypes() {
        try {
            return Arrays.asList(node.getMixinNodeTypes()).stream().map(DXGraphQLNodeType::new).collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isNodeType(@GraphQLName("anyType")Collection<String> anyType) {
        try {
            for (String type : anyType) {
                if (node.isNodeType(type)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public List<DXGraphQLProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                                 @GraphQLName("language") String language) {
        List<DXGraphQLProperty> propertyList = new ArrayList<DXGraphQLProperty>();
        try {
            JCRNodeWrapper node = this.node;
            if (language != null) {
                node = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(node.getIdentifier());
            }
            if (names != null && !names.isEmpty()) {
                for (String name : names) {
                    if (node.hasProperty(name)) {
                        propertyList.add(new DXGraphQLProperty(node.getProperty(name)));
                    }
                }
            } else {
                PropertyIterator pi = node.getProperties();
                while (pi.hasNext()) {
                    JCRPropertyWrapper property = (JCRPropertyWrapper) pi.nextProperty();
                    propertyList.add(new DXGraphQLProperty(property));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return propertyList;
    }

    @Override
    public DXGraphQLProperty getProperty(@GraphQLName("name") String name,
                                         @GraphQLName("language") String language) {
        try {
            JCRNodeWrapper node = this.node;
            if (language != null) {
                node = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language))
                        .getNodeByIdentifier(node.getIdentifier());
            }
            if (node.hasProperty(name)) {
                return new DXGraphQLProperty(node.getProperty(name));
            }
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @GraphQLField
//    @GraphQLConnection
    public List<DXGraphQLJCRNode> getChildren(@GraphQLName("names") Collection<String> names,
                                              @GraphQLName("anyType") Collection<String> anyType,
                                              @GraphQLName("properties") PropertyFilterType properties,
                                              @GraphQLName("asMixin") String asMixin) {
        List<DXGraphQLJCRNode> children = new ArrayList<DXGraphQLJCRNode>();
        try {
            Iterator<JCRNodeWrapper> nodes = IteratorUtils.filteredIterator(node.getNodes().iterator(), getNodesPredicate(names,anyType,properties));
            while (nodes.hasNext()) {
                children.add(new DXGraphQLGenericJCRNode(nodes.next()));
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return children;
    }

    private AllPredicate<JCRNodeWrapper> getNodesPredicate(final Collection<String> names, final Collection<String> anyType, final PropertyFilterType property) {
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
                        if (property.key == null) {
                            return true;
                        }
//                        if (properties == null || properties.isEmpty()) {
//                            return true;
//                        }
//                        for (PropertyFilterType property : properties) {
                            try {
                                if (!node.hasProperty(property.key) || !node.getProperty(property.key).getString().equals(property.value)) {
                                    return false;
                                }
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
//                        }
                        return true;
                    }
                }
        );
    }

    @Override
    public List<DXGraphQLJCRNode> getAncestors(@GraphQLName("upToPath") String upToPath) {
        List<DXGraphQLJCRNode> ancestors = new ArrayList<DXGraphQLJCRNode>();

        String upToPathSlash = upToPath + "/";

        try {
            List<JCRItemWrapper> jcrAncestors = node.getAncestors();
            for (JCRItemWrapper ancestor : jcrAncestors) {
                if (upToPath == null || ancestor.getPath().equals(upToPath) || ancestor.getPath().startsWith(upToPathSlash)) {
                    ancestors.add(new DXGraphQLGenericJCRNode((JCRNodeWrapper) ancestor));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return ancestors;
    }
}
