package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.functors.AllPredicate;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ChildrenDataFetcher implements DataFetcher {
    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
        DXGraphQLNode node = (DXGraphQLNode) dataFetchingEnvironment.getSource();
        String asMixin = dataFetchingEnvironment.getArgument("asMixin");

        List<DXGraphQLNode> children = new ArrayList<DXGraphQLNode>();
        try {
            Iterator<JCRNodeWrapper> nodes = IteratorUtils.filteredIterator(node.getNode().getNodes().iterator(), getNodesPredicate(dataFetchingEnvironment));
            while (nodes.hasNext()) {
                children.add(new DXGraphQLNode(nodes.next(), asMixin));
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return DXGraphQLBuilder.getList(children, "Node");
    }

    private AllPredicate<JCRNodeWrapper> getNodesPredicate(DataFetchingEnvironment dataFetchingEnvironment) {
        final List<String> names = dataFetchingEnvironment.getArgument("names");
        final List<String> anyType = dataFetchingEnvironment.getArgument("anyType");
        final List<Map> properties = dataFetchingEnvironment.getArgument("properties");

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
                        for (Map property : properties) {
                            String key = (String) property.get("key");
                            String value = (String) property.get("value");
                            try {
                                if (!node.hasProperty(key) || !node.getProperty(key).getString().equals(value)) {
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


}
