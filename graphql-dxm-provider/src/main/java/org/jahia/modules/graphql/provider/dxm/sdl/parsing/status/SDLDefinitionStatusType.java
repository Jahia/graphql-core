package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public enum SDLDefinitionStatusType {
    OK("OK"),
    MISSING_FIELDS("Type does not define any field"),
    MISSING_TYPE("%s gql type is unavailable"),
    MISSING_JCR_NODE_TYPE("%s node type was not found"),
    MISSING_JCR_PROPERTY("%s property is missing from node type"),
    MISSING_JCR_CHILD("%s child is missing from node type");

    private String message;

    SDLDefinitionStatusType(String message) {
        this.message = message;
    }

    public String getMessage(String param) {
        return String.format(message, param);
    }
}
