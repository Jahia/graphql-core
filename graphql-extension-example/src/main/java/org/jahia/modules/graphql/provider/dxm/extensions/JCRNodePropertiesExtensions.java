package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.extensions.node.GqlName;
import org.jahia.modules.graphql.provider.dxm.extensions.node.GqlProperty;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                jcrProp.setType(PropertyType.nameFromValue(propDefinition.getRequiredType()));
                jcrProp.setPrefix(propDefinition.getPrefix());

                jcrProps.add(jcrProp);
            }

        }catch(Exception e){
            e.printStackTrace();
            logger.error("failed to get note type by typeName ", typeName);
        }

        return jcrProps;
    }

    @GraphQLField
    public static List<GqlName> typeNameSpaces(){
        List<GqlName> nameSpaces = new ArrayList<>();
        Map<String, String> nameSpacesMap = NodeTypeRegistry.getInstance().getNamespaces();
        nameSpacesMap.keySet().forEach( key -> nameSpaces.add(new GqlName(key)));

        return nameSpaces;
    }

    @GraphQLField
    public static List<GqlName> typeNamesByPrefix(@GraphQLNonNull @GraphQLName("namePrefix") @GraphQLDescription("prefix of name")
                                                          String  prefix){
        List<GqlName> typeNames = new ArrayList<>();
        NodeTypeRegistry.getInstance().getAllNodeTypes().forEach(nodeType -> {
            if(nodeType.getName().startsWith(prefix)) typeNames.add(new GqlName(nodeType.getName()));
        });

        return typeNames;
    }

}
