package org.jahia.modules.graphql.provider.dxm;

import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.osgi.GraphQLConfigurationProvider;
import graphql.kickstart.servlet.osgi.GraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.config.RequestOperationLimitPreProcessor;
import org.osgi.service.component.annotations.Component;

/**
 * Supplies the builder the servlet assembles its configuration from, so that the servlet's own defaults are kept and
 * the bound on how many operations one request may submit is added to them.
 *
 * <p>Registered under {@link GraphQLProvider} as well as under the interface it implements, and that second
 * registration is load-bearing rather than decorative: binding a configuration provider makes the servlet rebuild its
 * schema, whereas binding a provider makes it rebuild both the schema and the configuration, and it is the
 * configuration that carries the pre-processor. Binding as a provider also sets the configuration builder provider
 * before it rebuilds, so a single bind is enough whichever of the two the servlet reaches first.
 */
@Component(service = {GraphQLConfigurationProvider.class, GraphQLProvider.class}, immediate = true)
public class JahiaGraphQLConfigurationProvider implements GraphQLConfigurationProvider {

    @Override
    public GraphQLConfiguration.Builder getConfigurationBuilder() {
        return new GraphQLConfiguration.Builder().with(new RequestOperationLimitPreProcessor());
    }
}
