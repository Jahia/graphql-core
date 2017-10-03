package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.*;
import org.jahia.services.content.JCRNodeWrapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@GraphQLName("JCRNode")
@GraphQLTypeResolver(SpecializedTypesHandler.NodeTypeResolver.class)
public interface GqlJcrNode {

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
    GqlJcrNode getParent();

    @GraphQLField()
    List<GqlJcrProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                             @GraphQLName("language") String language);

    @GraphQLField()
    GqlJcrProperty getProperty(@GraphQLName("name") String name,
                                     @GraphQLName("language") String language);

//    @GraphQLConnection()
    @GraphQLField()
    List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                       @GraphQLName("anyType") Collection<String> anyType,
                                       @GraphQLName("properties") Collection<PropertyFilterTypeInput> properties,
                                       @GraphQLName("asMixin") String asMixin);

    @GraphQLField()
    List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath);

    @GraphQLField()
    GqlJcrSite getSite();

    @GraphQLField()
    GqlJcrNode asMixin(@GraphQLName("type") String type);

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
