package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.kickstart.servlet.osgi.GraphQLProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import org.apache.felix.utils.collections.MapToDictionary;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.osgi.BundleUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
public class PublicationJobSubscriptionExtension implements EventHandler {

    static Logger logger = LoggerFactory.getLogger(PublicationJobSubscriptionExtension.class);


    private static ServiceRegistration<EventHandler> eventHandlerServiceRegistrationDone;
    private static ServiceRegistration<EventHandler> eventHandlerServiceRegistrationStart;
    private static BundleContext bundleContext;
    private static Map<String, FlowableEmitter<GqlPublicationEvent>> listeners = new HashMap<>();

    @GraphQLField
    @GraphQLDescription("Subscription on publication jobs")
    public static Publisher<GqlPublicationEvent> subscribeToPublicationJob() {
        if (bundleContext == null && eventHandlerServiceRegistrationDone == null) {
            bundleContext = ((DXGraphQLProvider) BundleUtils.getOsgiService(GraphQLProvider.class, null)).getBundleContext();
            new PublicationJobSubscriptionExtension().registerEventHandler();
        }
        return Flowable.create(obs -> {
            String name = UUID.randomUUID().toString();
            listeners.put(name, obs);
            logger.debug("Registered publication job listener {}", name);
            obs.setCancellable(() -> {
                logger.debug("Registered publication job listener {}", name);
                listeners.remove(name);
            });
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("Received event on topic {}", event.getTopic());
        GqlPublicationEvent gqlPublicationEvent = new GqlPublicationEvent(
                event.getTopic().endsWith("/publication/done") ? GqlPublicationEvent.State.FINISHED : GqlPublicationEvent.State.STARTED,
                (List<String>) event.getProperty("siteKey"),
                (List<String>) event.getProperty("language"),
                (List<String>) event.getProperty("paths"),
                (List<String>) event.getProperty("user"));
        listeners.forEach((name, emitter) -> emitter.onNext(gqlPublicationEvent));
    }

    private void registerEventHandler() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.osgi.framework.Constants.SERVICE_PID, PublicationJobSubscriptionExtension.class.getName() + ".EventHandler.Done");
        props.put(org.osgi.framework.Constants.SERVICE_DESCRIPTION,
                "Publication Job event handler");
        props.put(org.osgi.framework.Constants.SERVICE_VENDOR, "Jahia Solutions Group SA");
        props.put(EventConstants.EVENT_TOPIC, Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/done");

        eventHandlerServiceRegistrationDone = bundleContext.registerService(EventHandler.class, this, new MapToDictionary(props));
        props.put(EventConstants.EVENT_TOPIC, Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/start");
        props.put(org.osgi.framework.Constants.SERVICE_PID, PublicationJobSubscriptionExtension.class.getName() + ".EventHandler.Start");

        eventHandlerServiceRegistrationStart = bundleContext.registerService(EventHandler.class, this, new MapToDictionary(props));
    }

    private void unregisterEventHandler() {
        if (eventHandlerServiceRegistrationDone != null) {
            logger.info("Unregistering Event Handler");
            eventHandlerServiceRegistrationDone.unregister();
            eventHandlerServiceRegistrationStart.unregister();
        }
    }

}
