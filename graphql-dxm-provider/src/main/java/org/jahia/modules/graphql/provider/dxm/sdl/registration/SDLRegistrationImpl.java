package org.jahia.modules.graphql.provider.dxm.sdl.registration;

import org.jahia.modules.external.modules.osgi.ModulesSourceMonitor;
import org.jahia.modules.graphql.provider.dxm.sdl.monitor.SDLFileSourceMonitor;
import org.jahia.osgi.BundleUtils;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = SDLRegistrationService.class, immediate = true)
public class SDLRegistrationImpl implements SDLRegistrationService, SynchronousBundleListener {
    private final static Logger logger = LoggerFactory.getLogger(SDLRegistrationImpl.class);
    private static final String JAHIA_SOURCE_FOLDERS = "Jahia-Source-Folders";
    private static final String SRC_MAIN_RESOURCES = "/src/main/resources/";
    private static final String GRAPHQL_EXTENSION_SDL = "META-INF/graphql-extension.sdl";
    private final Map<String, URL> sdlResources = new ConcurrentHashMap<>();
    private BundleContext bundleContext;
    private ComponentContext componentContext;
    private Boolean hasModulesMonitoringActivated = Boolean.FALSE;

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

    private ServiceRegistration<ModulesSourceMonitor> serviceRegistration = null;

    @Activate
    public void activate(ComponentContext componentContext, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
        this.bundleContext.addBundleListener(this);
        boolean sdlResourcesDiscovered = false;

        //Detect if external-modules-provider provides ModulesSourceMonitor Interface
        Bundle modulesProvider = BundleUtils.getBundleBySymbolicName("external-provider-modules", null);
        registerSourceMonitorService(modulesProvider);

        for (Bundle bundle : bundleContext.getBundles()) {
            if (checkForSDLResourceInBundle(bundle, null)) {
                sdlResourcesDiscovered = true;
            }
        }
        if (sdlResourcesDiscovered) {
            componentContext.disableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
            componentContext.enableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
        }
    }

    @Deactivate
    public void deactivate() {
        unregisterSourceMonitorService();
    }

    @Override
    public Map<String, URL> getSDLResources() {
        return sdlResources;
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        //Handle specific case of external-provider-modules bundle
        int eventType = event.getType();
        if (event.getBundle().getSymbolicName().equals("external-provider-modules")) {
            if (eventType == BundleEvent.STARTED) {
                registerSourceMonitorService(event.getBundle());
            } else if (eventType == BundleEvent.STOPPED) {
                unregisterSourceMonitorService();
            }
        }

        if (checkForSDLResourceInBundle(bundle, event)) {
            logger.debug("received event {} for bundle {} ", new Object[]{status.get(eventType), event.getBundle().getSymbolicName()});
            if (eventType == BundleEvent.STARTED || eventType == BundleEvent.STOPPED) {
                componentContext.disableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
                componentContext.enableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
            }
        }
    }

    private boolean checkForSDLResourceInBundle(Bundle bundle, BundleEvent event) {
        if (BundleUtils.isJahiaBundle(bundle)) {
            if (event != null && event.getType() == BundleEvent.UNINSTALLED) {
                //Remove resource from bundle that was uninstalled
                registerSDLResource(event.getType(), bundle.getSymbolicName(), null);
            } else {
                URL url = bundle.getResource(GRAPHQL_EXTENSION_SDL);
                if (url != null) {
                    logger.debug("get bundle schema {}", new Object[]{url.getPath()});
                    if (hasModulesMonitoringActivated) {
                        String sourcesFolder = bundle.getHeaders().get(JAHIA_SOURCE_FOLDERS);
                        if (sourcesFolder != null) {
                            File file = new File(sourcesFolder + SRC_MAIN_RESOURCES + GRAPHQL_EXTENSION_SDL);
                            if (file.exists()) {
                                try {
                                    url = file.toURI().toURL();
                                } catch (MalformedURLException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    registerSDLResource(event != null ? event.getType() : bundle.getState() == Bundle.ACTIVE ? BundleEvent.STARTED : BundleEvent.UNINSTALLED, bundle.getSymbolicName(), url);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Registers or deregisters a bundle's graphql-extension.sdl in the sdl registry
     *
     * @param bundleEventType - Current state of the bundle
     * @param bundleName      - Name of the bundle
     * @param sdlResource     - URL resource pointing to SDL file (graphql-extension.sdl) in bundle
     */
    private void registerSDLResource(int bundleEventType, String bundleName, final URL sdlResource) {
        switch (bundleEventType) {
            case BundleEvent.STARTED:
                if (!sdlResources.containsKey(bundleName)) {
                    logger.debug("add new type registry for " + bundleName);
                    sdlResources.put(bundleName, sdlResource);
                }
                break;
            default:
                if (sdlResources.containsKey(bundleName)) {
                    logger.debug("remove type registry for " + bundleName);
                    sdlResources.remove(bundleName);
                }
        }
    }

    private void registerSourceMonitorService(Bundle modulesProvider) {
        try {
            hasModulesMonitoringActivated = Boolean.FALSE;
            if (modulesProvider != null && modulesProvider.loadClass("org.jahia.modules.external.modules.osgi.ModulesSourceMonitor") != null) {
                hasModulesMonitoringActivated = Boolean.TRUE;
                unregisterSourceMonitorService();
                serviceRegistration = bundleContext.registerService(ModulesSourceMonitor.class, new SDLFileSourceMonitor(bundleContext, componentContext), null);
            }
        } catch (ClassNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void unregisterSourceMonitorService() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
