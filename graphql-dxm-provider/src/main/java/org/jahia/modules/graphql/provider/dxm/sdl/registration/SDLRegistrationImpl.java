package org.jahia.modules.graphql.provider.dxm.sdl.registration;

import org.jahia.modules.external.modules.osgi.ModulesSourceMonitor;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.sdl.monitor.SDLFileSourceMonitor;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
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
    private static final String EXTERNAL_PROVIDER_MODULES = "external-provider-modules";
    private static final Logger logger = LoggerFactory.getLogger(SDLRegistrationImpl.class);
    private static final String JAHIA_SOURCE_FOLDERS = "Jahia-Source-Folders";
    private static final String SRC_MAIN_RESOURCES = "/src/main/resources/";
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
    private ServiceRegistration<ModulesSourceMonitor> serviceRegistration = null;

    @Activate
    public void activate(ComponentContext componentContext, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
        this.bundleContext.addBundleListener(this);

        registerSourceMonitorService();
        registerAllBundles(isSourcesAvailable());

        BundleUtils.getBundleBySymbolicName(EXTERNAL_PROVIDER_MODULES, null);
    }

    @Deactivate
    public void deactivate() {
        unregisterSourceMonitorService();
    }

    @Override
    public Map<String, URL> getSDLResources() {
        return sdlResources;
    }

    private boolean isSourcesAvailable() {
        return ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById(EXTERNAL_PROVIDER_MODULES) != null;
    }

    private void registerAllBundles(boolean sourcesAvailable) {
        sdlResources.clear();
        for (Bundle bundle : bundleContext.getBundles()) {
            checkForSDLResourceInBundle(bundle, bundle.getState() == Bundle.ACTIVE, sourcesAvailable);
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        int eventType = event.getType();
        Bundle bundle = event.getBundle();
        boolean restart = false;
        if (eventType == BundleEvent.STARTED || eventType == BundleEvent.STOPPED) {
            if (bundle.getSymbolicName().equals(EXTERNAL_PROVIDER_MODULES)) {
                logger.debug("received event {} for bundle {}, reloading all sdl", status.get(eventType), bundle.getSymbolicName());
                registerAllBundles(eventType == BundleEvent.STARTED);
                restart = true;
            } else if (checkForSDLResourceInBundle(bundle, eventType == BundleEvent.STARTED, isSourcesAvailable())) {
                logger.debug("received event {} for bundle {} ", status.get(eventType), bundle.getSymbolicName());
                restart = true;
            }
        }
        if (restart) {
            componentContext.disableComponent(DXGraphQLProvider.class.getName());
            componentContext.enableComponent(DXGraphQLProvider.class.getName());
        }
    }

    private boolean checkForSDLResourceInBundle(Bundle bundle, boolean register, boolean withSources) {
        if (BundleUtils.isJahiaBundle(bundle) && bundle.getState() >= Bundle.RESOLVED) {
            URL url = bundle.getResource(GRAPHQL_EXTENSION_SDL);
            if (url != null) {
                logger.debug("get bundle schema {}", url.getPath());
                if (withSources) {
                    url = getSourcesUrl(bundle, url);
                }
                handleSDLResource(register, bundle.getSymbolicName(), url);
                return true;
            }
        }
        return false;
    }

    private URL getSourcesUrl(Bundle bundle, URL url) {
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
        return url;
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

    private void registerSourceMonitorService() {
        try {
            serviceRegistration = bundleContext.registerService(ModulesSourceMonitor.class, new SDLFileSourceMonitor(bundleContext, componentContext), null);
        } catch (NoClassDefFoundError e) {
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
