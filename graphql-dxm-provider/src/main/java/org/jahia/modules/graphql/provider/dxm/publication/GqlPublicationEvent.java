package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;

import java.util.List;

public class GqlPublicationEvent {
    private final State state;
    private final String siteKey;
    private final String language;
    private final List<String> paths;
    private final String user;

    public enum State {FINISHED, STARTED}

    public GqlPublicationEvent(State state, List<String> siteKey, List<String> language, List<String> paths, List<String> user) {
        this.state = state;
        this.siteKey = getString(siteKey);
        this.language = getString(language);
        this.paths = paths;
        this.user = getString(user);
    }

    private static String getString(List<String> list) {
        return list != null ? list.stream().findFirst().orElse(null) : null;
    }

    @GraphQLField
    @GraphQLDescription("State of the publication event")
    public State getState() {
        return state;
    }

    @GraphQLField
    @GraphQLDescription("Site key")
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLDescription("Language")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLDescription("Paths")
    public List<String> getPaths() {
        return paths;
    }

    @GraphQLField
    @GraphQLDescription("User")
    public String getUser() {
        return user;
    }
}
