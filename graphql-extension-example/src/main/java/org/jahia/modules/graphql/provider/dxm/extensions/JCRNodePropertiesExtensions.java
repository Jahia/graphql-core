package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.extensions.node.GqlProperty;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created at Nov 2018$
 *
 * @author chooliyip
 **/

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("a GraphQL extension of note type properties")
public class JCRNodePropertiesExtensions {

    private static final Logger logger = LoggerFactory.getLogger(JCRNodePropertiesExtensions.class);

    @GraphQLField
    public static List<GqlProperty> nodeTypeProps(@GraphQLNonNull @GraphQLName("name") @GraphQLDescription("name of specific node type")
                                            String typeName){

        List<GqlProperty> jcrProps = new ArrayList<>();

        try{
            ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(typeName);

            ExtendedPropertyDefinition[] propDefinitions = type.getPropertyDefinitions();
            for(ExtendedPropertyDefinition propDefinition : propDefinitions){
                GqlProperty jcrProp = new GqlProperty();

                jcrProp.setName(propDefinition.getName());
                jcrProp.setType(propDefinition.getItemType());

                jcrProps.add(jcrProp);
            }

        }catch(Exception e){
            e.printStackTrace();
            logger.error("failed to get note type by typeName ", typeName);
        }

        return jcrProps;
    }

}
