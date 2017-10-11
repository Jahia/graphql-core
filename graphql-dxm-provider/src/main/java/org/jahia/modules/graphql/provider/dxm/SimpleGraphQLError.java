package org.jahia.modules.graphql.provider.dxm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

public class SimpleGraphQLError implements GraphQLError {

    private final String message;
    private List<Object> path;
    private List<SourceLocation> locations;
    private ErrorType errorType;

    public SimpleGraphQLError(String message, List<Object> path, List<SourceLocation> locations, ErrorType errorType) {
        this.message = message;
        this.path = path;
        this.locations = locations;
        this.errorType = errorType;
    }


    @Override
    public List<Object> getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getExtensions() {
        return null;
    }
}
