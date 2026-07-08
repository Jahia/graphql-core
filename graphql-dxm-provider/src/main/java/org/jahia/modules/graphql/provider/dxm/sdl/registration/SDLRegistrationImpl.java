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
package org.jahia.modules.graphql.provider.dxm.sdl.registration;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.osgi.BundleUtils;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = SDLRegistrationService.class, immediate = true)
public class SDLRegistrationImpl implements SDLRegistrationService, SynchronousBundleListener {
    private static final Logger logger = LoggerFactory.getLogger(SDLRegistrationImpl.class);
    private static final String GRAPHQL_EXTENSION_SDL = "META-INF/graphql-extension.sdl";
    private static Map<Integer, String> status = new HashMap<>();

    static {
        status.put(BundleEvent.INSTALLED, "installed");
        status.put(BundleEvent.UNINSTALLED, "uninstalled");
        status.put(BundleEvent.RESOLVED, "resolved");
        status.put(BundleEvent.UNRESOLVED, "unresolved");
        status.put(BundleEvent.STARTED, "started");
        status.put(BundleEvent.STARTING, "starting");
        status.put(BundleEvent.STOPPED, "stopped");
        status.put(BundleEvent.STOPPING, "started");
        status.put(BundleEvent.UPDATED, "updated");
    }

    private final Map<String, URL> sdlResources = new ConcurrentHashMap<>();
    private BundleContext bundleContext;
    private ComponentContext componentContext;

    @Activate
    public void activate(ComponentContext componentContext, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
        this.bundleContext.addBundleListener(this);
        registerAllBundles();
    }

    @Deactivate
    public void deactivate() {
        if (bundleContext != null) {
            bundleContext.removeBundleListener(this);
        }
    }

    @Override
    public Map<String, URL> getSDLResources() {
        return sdlResources;
    }

    private void registerAllBundles() {
        sdlResources.clear();
        for (Bundle bundle : bundleContext.getBundles()) {
            checkForSDLResourceInBundle(bundle, bundle.getState() == Bundle.ACTIVE);
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        int eventType = event.getType();
        Bundle bundle = event.getBundle();
        if (eventType == BundleEvent.STARTED || eventType == BundleEvent.STOPPED) {
            if (checkForSDLResourceInBundle(bundle, eventType == BundleEvent.STARTED)) {
                logger.debug("received event {} for bundle {} ", status.get(eventType), bundle.getSymbolicName());
                componentContext.disableComponent(DXGraphQLProvider.class.getName());
                componentContext.enableComponent(DXGraphQLProvider.class.getName());
            }
        }
    }

    private boolean checkForSDLResourceInBundle(Bundle bundle, boolean register) {
        if (BundleUtils.isJahiaBundle(bundle) && bundle.getState() >= Bundle.RESOLVED) {
            URL url = bundle.getResource(GRAPHQL_EXTENSION_SDL);
            if (url != null) {
                logger.debug("get bundle schema {}", url.getPath());
                handleSDLResource(register, bundle.getSymbolicName(), url);
                return true;
            }
        }
        return false;
    }

    /**
     * Registers or deregisters a bundle's graphql-extension.sdl in the sdl registry
     *
     * @param register        - Register or unregister
     * @param bundleName      - Name of the bundle
     * @param sdlResource     - URL resource pointing to SDL file (graphql-extension.sdl) in bundle
     */
    private void handleSDLResource(boolean register, String bundleName, final URL sdlResource) {
        if (register) {
            if (!sdlResources.containsKey(bundleName)) {
                logger.debug("add new type registry for {}", bundleName);
                sdlResources.put(bundleName, sdlResource);
            }
        } else {
            if (sdlResources.containsKey(bundleName)) {
                logger.debug("remove type registry for {}", bundleName);
                sdlResources.remove(bundleName);
            }
        }
    }
}
