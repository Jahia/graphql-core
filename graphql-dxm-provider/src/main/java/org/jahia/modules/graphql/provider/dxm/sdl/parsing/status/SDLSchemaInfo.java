package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public class SDLSchemaInfo {
    public enum SDLSchemaStatus {
        OK,
        SYNTAX_ERROR,
        DEFINITION_ERROR
    }
    private String error;
    private String bundle;
    private SDLSchemaStatus status;

    public SDLSchemaInfo(String bundle) {
        this(bundle, SDLSchemaStatus.OK);
    }

    public SDLSchemaInfo(String bundle, SDLSchemaStatus status) {
        this(bundle, status, null);
    }

    public SDLSchemaInfo(String bundle, SDLSchemaStatus status, String error) {
        this.bundle = bundle;
        this.status = status;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public SDLSchemaStatus getStatus() {
        return status;
    }

    public void setStatus(SDLSchemaStatus status) {
        this.status = status;
    }
}
