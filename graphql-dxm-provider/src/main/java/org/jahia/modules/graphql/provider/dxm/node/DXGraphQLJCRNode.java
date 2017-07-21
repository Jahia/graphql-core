package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.*;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRNode")
@GraphQLTypeResolver(SpecializedTypesHandler.NodeTypeResolver.class)
public interface DXGraphQLJCRNode {

    JCRNodeWrapper getNode();

    String getType();

    @GraphQLField()
    @GraphQLDescription("Unique identifier")
    String getUuid();

    @GraphQLField()
    @GraphQLDescription("The name of the node")
    String getName();

    @GraphQLField()
    @GraphQLDescription("The path of the node")
    String getPath();

    @GraphQLField()
    String getDisplayName(@GraphQLName("language") String language);

    @GraphQLField()
    DXGraphQLJCRNode getParent();

    @GraphQLField()
    List<DXGraphQLJCRProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                             @GraphQLName("language") String language);

    @GraphQLField()
    DXGraphQLJCRProperty getProperty(@GraphQLName("name") String name,
                                     @GraphQLName("language") String language);

//    @GraphQLConnection()
    @GraphQLField()
    List<DXGraphQLJCRNode> getChildren(@GraphQLName("names") Collection<String> names,
                                       @GraphQLName("anyType") Collection<String> anyType,
                                       @GraphQLName("properties") Collection<PropertyFilterTypeInput> properties,
                                       @GraphQLName("asMixin") String asMixin);

    @GraphQLField()
    List<DXGraphQLJCRNode> getAncestors(@GraphQLName("upToPath") String upToPath);

    @GraphQLField()
    DXGraphQLJCRSite getSite();

    @GraphQLField()
    DXGraphQLJCRNode asMixin(@GraphQLName("type") String type);

    public class PropertyFilterTypeInput {

        public PropertyFilterTypeInput(HashMap m) {
            if (m != null) {
                this.key = (String) m.get("key");
                this.value = (String) m.get("value");
            }
        }

        @GraphQLField()
        public String key;

        @GraphQLField()
        public String value;
    }

}
