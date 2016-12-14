package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.schema.*;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLExtender;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.render.RenderContext;
import org.osgi.service.component.annotations.Component;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

@Component(service = DXGraphQLExtender.class, immediate = true, property = { "graphQLType=node"})
public class DisplayableNodeExtender implements DXGraphQLExtender {

    @Override
    public GraphQLObjectType.Builder build(GraphQLObjectType.Builder builder) {
        return builder.field(GraphQLFieldDefinition.newFieldDefinition()
                .name("displayableNode")
                .type(new GraphQLTypeReference("node"))
                .dataFetcher(getDisplayableNodePathDataFetcher())
                .build());
    }


    public DataFetcher getDisplayableNodePathDataFetcher()  {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DXGraphQLNode) {
                    RenderContext context = new RenderContext(((GraphQLContext)environment.getContext()).getRequest().get(),
                            ((GraphQLContext)environment.getContext()).getResponse().get(),
                            JCRSessionFactory.getInstance().getCurrentUser());
                    JCRNodeWrapper node = JCRContentUtils.findDisplayableNode(((DXGraphQLNode) environment.getSource()).getNode(), context);
                    if (node != null) {
                        return new DXGraphQLNode(node);
                    } else {
                        return null;
                    }
                }
                return null;
            }
        };
    }

}
