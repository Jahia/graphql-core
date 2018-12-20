package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public class SDLDefinitionStatus {
    private String name;
    private String mapsToType;
    private String mappedTypeModuleName;
    private String mappedTypeModuleId;
    private SDLDefinitionStatusTypes statusType;
    private SDLDefinitionStatusDescription statusDescription;

    public SDLDefinitionStatus(String name, SDLDefinitionStatusTypes status) {
        this.name = name;
        this.statusType = status;
        this.statusDescription = new SDLDefinitionStatusDescription(status);
    }

    public SDLDefinitionStatus(String name, String mapsToType, String mappedTypeModuleName, String mappedTypeModuleId, SDLDefinitionStatusTypes status) {
        this.name = name;
        this.mapsToType = mapsToType;
        this.mappedTypeModuleName = mappedTypeModuleName;
        this.mappedTypeModuleId = mappedTypeModuleId;
        this.statusType = status;
        this.statusDescription = new SDLDefinitionStatusDescription(status);
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

    public void setStatusType(SDLDefinitionStatusTypes statusType) {
        this.statusType = statusType;
    }

    public void setStatusDescription(SDLDefinitionStatusDescription statusDescription) {
        statusDescription.setMessage(statusType);
        this.statusDescription = statusDescription;
    }

    @Override
    public String toString() {
        return String.format("DEFINITION: %s maps to type %s from module %s with id %s. STATUS: %s",
                this.name,
                this.mapsToType,
                this.mappedTypeModuleName,
                this.mappedTypeModuleId,
                this.statusDescription.toString());
    }

    public String getStatus() {
        switch(this.statusType) {
            case MISSING_JCR_PROPERTY:
                return "MISSING_JCR_PROPERTY";
            case MISSING_JCR_NODE_TYPE:
                return "MISSING_JCR_NODE_TYPE";
            case OK:
            default:
                return "OK";
        }
    }
}
