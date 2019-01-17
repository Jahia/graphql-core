package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public class SDLDefinitionStatus {
    private String name;
    private String mapsToType;
    private String mappedTypeModuleName;
    private String mappedTypeModuleId;
    private String statusParam;
    private SDLDefinitionStatusType statusType;

    public SDLDefinitionStatus(String name, SDLDefinitionStatusType status) {
        this.name = name;
        this.statusType = status;
    }

    public SDLDefinitionStatus(String name, SDLDefinitionStatusType status, String statusParam) {
        this.name = name;
        this.statusType = status;
        this.statusParam = statusParam;
    }

    public void setMapsToType(String mapsToType) {
        this.mapsToType = mapsToType;
    }

    public void setMappedTypeModuleName(String mappedTypeModuleName) {
        this.mappedTypeModuleName = mappedTypeModuleName;
    }

    public void setMappedTypeModuleId(String mappedTypeModuleId) {
        this.mappedTypeModuleId = mappedTypeModuleId;
    }

    public void setStatusType(SDLDefinitionStatusType statusType, String statusParam) {
        this.statusType = statusType;
        this.statusParam = statusParam;
    }

    @Override
    public String toString() {
        return String.format("DEFINITION: %s maps to type %s from module %s with id %s. STATUS: %s",
                this.name,
                this.mapsToType,
                this.mappedTypeModuleName,
                this.mappedTypeModuleId,
                getStatusString());
    }

    public String getStatusString() {
        return statusType.getMessage(statusParam);
    }

    public SDLDefinitionStatusType getStatus() {
        return statusType;
    }
}
