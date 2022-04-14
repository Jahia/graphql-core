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
package org.jahia.modules.graphql.provider.dxm.sdl.monitor;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jahia.modules.external.modules.osgi.ModulesSourceMonitor;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Class monitoring for SDL files on local installation
 */
public class SDLFileSourceMonitor implements ModulesSourceMonitor {
    private static Logger logger = LoggerFactory.getLogger(SDLFileSourceMonitor.class);
    private BundleContext bundleContext;
    private ComponentContext componentContext;

    public SDLFileSourceMonitor(BundleContext bundleContext, ComponentContext componentContext) {
        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
    }

    @Override
    public boolean canHandleFileType(FileObject filePath) {
        try {
            FileObject parent = filePath.getParent();
            if (parent != null) {
                String baseName = filePath.getName().getBaseName();
                String parentName = parent.getName().getBaseName();
                if (baseName.equals("graphql-extension.sdl") && parentName.equals("META-INF")) {
                    logger.info("We can handle file {}", baseName);
                    return true;
                }
            }
        } catch (FileSystemException e) {
            logger.error("Cannot read file", e);
        }
        return false;
    }

    @Override
    public void handleFile(File file) {
        logger.debug("Processing file {}", file);
        componentContext.disableComponent(DXGraphQLProvider.class.getName());
        componentContext.enableComponent(DXGraphQLProvider.class.getName());
    }
}
