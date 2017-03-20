package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLTypeReference;
import graphql.servlet.GraphQLContext;
import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLExtender;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.render.RenderContext;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.List;

import static graphql.Scalars.GraphQLString;

@Component(service = DXGraphQLExtender.class, immediate = true, property = {"graphQLType=node"})
public class DisplayableNodeExtender implements DXGraphQLExtender {

    @Override
    public List<GraphQLFieldDefinition> getFields() {
        return Arrays.asList(
                GraphQLFieldDefinition.newFieldDefinition()
                        .name("displayableNode")
                        .type(new GraphQLTypeReference("node"))
                        .dataFetcher(getDisplayableNodePathDataFetcher())
                        .build(),
                GraphQLFieldDefinition.newFieldDefinition()
                        .name("ajaxRenderUrl")
                        .type(GraphQLString)
                        .dataFetcher(getAjaxRenderUrl())
                        .build()
        );
    }


    public DataFetcher getDisplayableNodePathDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DXGraphQLNode) {
                    RenderContext context = new RenderContext(((GraphQLContext) environment.getContext()).getRequest().get(),
                            ((GraphQLContext) environment.getContext()).getResponse().get(),
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

    public DataFetcher getAjaxRenderUrl() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DXGraphQLNode) {
                    DXGraphQLNode node = (DXGraphQLNode) environment.getSource();
                    return node.getNode().getUrl() + ".ajax";
                }
                return null;
            }
        };
    }

}
