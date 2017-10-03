package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.services.content.JCRNodeWrapper;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.annotations.GraphQLNonNull;
import graphql.annotations.GraphQLTypeResolver;

import java.util.Collection;
import java.util.List;

@GraphQLTypeResolver(SpecializedTypesHandler.NodeTypeResolver.class)
public interface GqlJcrNode {

    JCRNodeWrapper getNode();

    String getType();

    @GraphQLField
    @GraphQLNonNull
    String getUuid();

    @GraphQLField
    @GraphQLNonNull
    String getName();

    @GraphQLField
    @GraphQLNonNull
    String getPath();

    @GraphQLField
    String getDisplayName(@GraphQLName("language") String language);

    @GraphQLField
    GqlJcrNode getParent();

    @GraphQLField
    @GraphQLNonNull
    Collection<GqlJcrProperty> getProperties(@GraphQLName("names") Collection<String> names,
                                             @GraphQLName("language") String language);

    @GraphQLField
    GqlJcrProperty getProperty(@GraphQLName("name") String name,
                               @GraphQLName("language") String language);

    @GraphQLField
    @GraphQLNonNull
    List<GqlJcrNode> getChildren(@GraphQLName("names") Collection<String> names,
                                 @GraphQLName("anyType") Collection<String> anyType,
                                 @GraphQLName("properties") Collection<PropertyFilterTypeInput> properties,
                                 @GraphQLName("asMixin") String asMixin);

    @GraphQLField
    @GraphQLNonNull
    List<GqlJcrNode> getAncestors(@GraphQLName("upToPath") String upToPath);

    @GraphQLField
    @GraphQLNonNull
    GqlJcrSite getSite();

    @GraphQLField
    GqlJcrNode asMixin(@GraphQLName("type") String type);

    public static class PropertyFilterTypeInput {

        public PropertyFilterTypeInput(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @GraphQLField
        public String key;

        @GraphQLField
        public String value;
    }
}
