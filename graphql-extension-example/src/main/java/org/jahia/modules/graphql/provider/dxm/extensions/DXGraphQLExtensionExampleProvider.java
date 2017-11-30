package org.jahia.modules.graphql.provider.dxm.extensions;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Collection;


@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class DXGraphQLExtensionExampleProvider implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.<Class<?>>asList(JCRNodeExtensions.class, QueryExtensions.class);
    }
}
