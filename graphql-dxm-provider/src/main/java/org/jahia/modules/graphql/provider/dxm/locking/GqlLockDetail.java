package org.jahia.modules.graphql.provider.dxm.locking;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GqlLockDetail {
    private static final Logger logger = LoggerFactory.getLogger(GqlLockDetail.class);
    private String language;
    private String owner;
    private String type;

    public GqlLockDetail(String language, String owner, String type) {
        this.language = language;
        this.owner = owner;
        this.type = type;
    }

    @GraphQLField
    @GraphQLName("language")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLName("owner")
    public String getOwner() {
        return owner;
    }

    @GraphQLField
    @GraphQLName("type")
    public String getType() {
        return type;
    }
}
