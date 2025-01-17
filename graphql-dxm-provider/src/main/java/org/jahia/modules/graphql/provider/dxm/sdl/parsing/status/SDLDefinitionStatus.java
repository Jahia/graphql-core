/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

public class SDLDefinitionStatus {
    private String name;
    private String mapsToType;
    private String mappedTypeModuleName;
    private String mappedTypeModuleId;
    private String[] statusParam;
    private SDLDefinitionStatusType statusType;

    public SDLDefinitionStatus(String name, SDLDefinitionStatusType status) {
        this.name = name;
        this.statusType = status;
    }

    public SDLDefinitionStatus(String name, SDLDefinitionStatusType status, String ...statusParam) {
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

    public void setStatusType(SDLDefinitionStatusType statusType, String ...statusParam) {
        this.statusType = statusType;
        this.statusParam = statusParam;
    }

    @Override
    public String toString() {
        String info = String.format("%s maps to type %s", this.name, this.mapsToType);
        if (mappedTypeModuleName != null) {
            info += String.format(" from module %s with id %s", this.mappedTypeModuleName, this.mappedTypeModuleId);
        }
        return String.format("DEFINITION: %s. STATUS: %s", info, getStatusString());
    }

    public String getStatusString() {
        return statusType.getMessage(statusParam);
    }

    public SDLDefinitionStatusType getStatus() {
        return statusType;
    }
}
