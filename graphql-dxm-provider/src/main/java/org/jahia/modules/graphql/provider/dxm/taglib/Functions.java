package org.jahia.modules.graphql.provider.dxm.taglib;

import org.jahia.modules.graphql.provider.dxm.sdl.parsing.SDLSchemaService;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLSchemaInfo;
import org.jahia.osgi.BundleUtils;

import java.util.List;
import java.util.Map;

public class Functions {

    public static Map<String, SDLDefinitionStatus> getSDLDefinitionsStatus() {
        SDLSchemaService sdlSchemaService = BundleUtils.getOsgiService(SDLSchemaService.class, null);
        return sdlSchemaService.getSdlDefinitionStatusMap();
    }

    public static Map<String, List<SDLSchemaInfo>> getBundlesSDLSchemaStatus() {
        SDLSchemaService sdlSchemaService = BundleUtils.getOsgiService(SDLSchemaService.class, null);
        return sdlSchemaService.getBundlesSDLSchemaStatus();
    }
}
