package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.servlet.GraphQLAnnotatedClassProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Collection;


@Component(service = graphql.servlet.GraphQLAnnotatedClassProvider.class, immediate = true)
public class DXGraphQLExtensionExampleProvider implements GraphQLAnnotatedClassProvider {
    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(JCRNodeExtensions.class, QueryExtensions.class);
    }
}
