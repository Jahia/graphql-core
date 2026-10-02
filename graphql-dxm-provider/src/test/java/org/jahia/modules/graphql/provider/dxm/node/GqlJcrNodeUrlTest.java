package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.processor.retrievers.GraphQLObjectInfoRetriever;
import graphql.annotations.processor.searchAlgorithms.BreadthFirstSearch;
import graphql.annotations.processor.searchAlgorithms.SearchAlgorithm;
import org.jahia.modules.graphql.provider.dxm.service.vanity.GqlJcrVanityUrl;
import org.jahia.modules.graphql.provider.dxm.site.GqlJcrSite;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.jcr.VanityUrlManager;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GqlJcrNodeUrlTest {

    private static final String URL = "/files/default/sites/demo/files/photo.jpg";
    private static final String SIZED_URL = "/files/default/sites/demo/files/photo.jpg?w=640";
    private static final List<String> PARAMS = Arrays.asList("w:640", "q:80");

    private static JCRNodeWrapper node() {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.getUrl()).thenReturn(URL);
        when(node.getUrl(anyList())).thenReturn(SIZED_URL);
        return node;
    }

    @Test
    public void urlWithParamsIsAnsweredByTheNode() {
        JCRNodeWrapper node = node();

        assertEquals(SIZED_URL, new GqlJcrNodeImpl(node).getUrl(PARAMS));
        verify(node).getUrl(PARAMS);
    }

    @Test
    public void urlWithoutParamsIsUnchanged() {
        JCRNodeWrapper node = node();
        GqlJcrNode gqlNode = new GqlJcrNodeImpl(node);

        assertEquals(URL, gqlNode.getUrl());
        assertEquals(URL, gqlNode.getUrl(null));
        assertEquals(URL, gqlNode.getUrl(Collections.emptyList()));
        verify(node, never()).getUrl(anyList());
    }

    @Test
    public void vanityUrlIgnoresParams() {
        JCRNodeWrapper node = node();
        when(node.getPropertyAsString(VanityUrlManager.PROPERTY_URL)).thenReturn("/promo");
        GqlJcrVanityUrl vanityUrl = new GqlJcrVanityUrl(node);

        assertEquals("/promo", vanityUrl.getUrl());
        assertEquals("/promo", vanityUrl.getUrl(PARAMS));
        verify(node, never()).getUrl(anyList());
    }

    @Test
    public void eachNodeTypeExposesOneUrlFieldWithParams() throws Exception {
        SearchAlgorithm graphQLFieldSearch = new BreadthFirstSearch(new GraphQLObjectInfoRetriever());
        for (Class<?> type : Arrays.asList(GqlJcrNode.class, GqlJcrNodeImpl.class, GqlJcrSite.class, GqlJcrVanityUrl.class)) {
            List<Method> urlFields = new ArrayList<>();
            for (Method method : type.getMethods()) {
                if (method.getName().equals("getUrl") && !method.isBridge() && graphQLFieldSearch.isFound(method)) {
                    urlFields.add(method);
                }
            }

            assertEquals(type.getSimpleName(), 1, urlFields.size());
            Method urlField = urlFields.get(0);
            assertEquals(type.getSimpleName(), Collections.singletonList(List.class), Arrays.asList(urlField.getParameterTypes()));
            assertEquals(type.getSimpleName(), "params", urlField.getParameters()[0].getAnnotation(GraphQLName.class).value());
        }
    }
}
