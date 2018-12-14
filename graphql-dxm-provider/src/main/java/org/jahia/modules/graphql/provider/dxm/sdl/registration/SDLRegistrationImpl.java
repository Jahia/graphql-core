package org.jahia.modules.graphql.provider.dxm.sdl.registration;

import org.jahia.osgi.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = SDLRegistrationService.class, immediate = true)
public class SDLRegistrationImpl implements SDLRegistrationService, SynchronousBundleListener {
    private final static Logger logger = LoggerFactory.getLogger(SDLRegistrationImpl.class);
    private final Map<String, URL> sdlResources = new ConcurrentHashMap<>();
    private BundleContext bundleContext;
    private ComponentContext componentContext;

    private static Map<Integer,String> status = new HashMap<>();

    static {
        status.put(BundleEvent.INSTALLED,"installed");
        status.put(BundleEvent.UNINSTALLED,"uninstalled");
        status.put(BundleEvent.RESOLVED,"resolved");
        status.put(BundleEvent.UNRESOLVED,"unresolved");
        status.put(BundleEvent.STARTED,"started");
        status.put(BundleEvent.STARTING,"starting");
        status.put(BundleEvent.STOPPED,"stopped");
        status.put(BundleEvent.STOPPING,"started");
        status.put(BundleEvent.UPDATED,"updated");
    }

    @Activate
    public void activate(ComponentContext componentContext, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
        this.bundleContext.addBundleListener(this);
        boolean sdlResourcesDiscovered = false;
        for (Bundle bundle: bundleContext.getBundles()) {
            if (checkForSDLResourceInBundle(bundle)) {
                sdlResourcesDiscovered = true;
            }
        }
        if (sdlResourcesDiscovered) {
            componentContext.disableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
            componentContext.enableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
        }
    }
    @Override
    public Map<String, URL> getSDLResources() {
        return sdlResources;
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (checkForSDLResourceInBundle(bundle)) {
            logger.debug("received event {} for bundle {} ",new Object[]{status.get(event.getType()),event.getBundle().getSymbolicName()});
            if(event.getType() == BundleEvent.STARTED || event.getType() == BundleEvent.STOPPED) {
                componentContext.disableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
                componentContext.enableComponent("org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider");
            }
        }
    }

    private boolean checkForSDLResourceInBundle(Bundle bundle) {
        if (BundleUtils.isJahiaBundle(bundle)) {
            URL url = bundle.getResource("META-INF/graphql-extension.sdl");
            if(url != null){
                logger.debug("get bundle schema {}",new Object[]{url.getPath()});
                registerSDLResource(bundle.getState(), bundle.getSymbolicName(), url);
                return true;
            }
        }
        return false;
    }

    /**
     * Registers or deregisters a bundle's graphql-extension.sdl in the sdl registry
     * @param bundleEventType - Current state of the bundle
     * @param bundleName - Name of the bundle
     * @param sdlResource - URL resource pointing to SDL file (graphql-extension.sdl) in bundle
     */
    private void registerSDLResource(int bundleEventType, String bundleName , final URL sdlResource){
        switch (bundleEventType) {
            case BundleEvent.STOPPED:
            case BundleEvent.STOPPING:
            case BundleEvent.UNINSTALLED:
            case BundleEvent.UNRESOLVED:
                if(sdlResources.containsKey(bundleName)) {
                    logger.debug("remove type registry for " + bundleName);
                    sdlResources.remove(bundleName);
                }
            case BundleEvent.RESOLVED:
            case BundleEvent.STARTED:
                if(!sdlResources.containsKey(bundleName)) {
                    logger.debug("add new type registry for " + bundleName);
                    sdlResources.put(bundleName, sdlResource);
                }
        }
    }
}
