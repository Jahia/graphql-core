package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.osgi.service.event.Event;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
public class PublicationJobSubscriptionExtension {

    static Logger logger = LoggerFactory.getLogger(PublicationJobSubscriptionExtension.class);

    private static final Map<String, FlowableEmitter<GqlPublicationEvent>> listeners = new HashMap<>();

    private PublicationJobSubscriptionExtension() {
        throw new IllegalStateException("Subscription class are fully static and must not be instantiated");
    }

    @GraphQLField
    @GraphQLDescription("Subscription on publication jobs")
    public static Publisher<GqlPublicationEvent> subscribeToPublicationJob() {
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

    public static void notifyListeners(Event event) {
        if (!listeners.isEmpty()) {
            logger.debug("Received event on topic {}", event.getTopic());
            GqlPublicationEvent gqlPublicationEvent = new GqlPublicationEvent(
                    event.getTopic().endsWith("/publication/done") ? GqlPublicationEvent.State.FINISHED : GqlPublicationEvent.State.STARTED,
                    (List<String>) event.getProperty("siteKey"),
                    (List<String>) event.getProperty("language"),
                    (List<String>) event.getProperty("paths"),
                    (List<String>) event.getProperty("user"));
            listeners.forEach((name, emitter) -> emitter.onNext(gqlPublicationEvent));
        }
    }

}
