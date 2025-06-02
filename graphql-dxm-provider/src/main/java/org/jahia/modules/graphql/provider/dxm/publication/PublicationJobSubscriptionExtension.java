package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.scheduler.jobs.GqlBackgroundJob;
import org.osgi.service.event.Event;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@GraphQLTypeExtension(DXGraphQLProvider.Subscription.class)
public class PublicationJobSubscriptionExtension {

    static Logger logger = LoggerFactory.getLogger(PublicationJobSubscriptionExtension.class);

    private static final Map<String, Map<String, Object>> listeners = new HashMap<>();

    private PublicationJobSubscriptionExtension() {
        throw new IllegalStateException("Subscription class are fully static and must not be instantiated");
    }

    @GraphQLField
    @GraphQLDescription("Subscription on publication jobs")
    public static Publisher<GqlPublicationEvent> subscribeToPublicationJob(@GraphQLName("userKeyFilter") @GraphQLDescription("Subscribe only to job with matching user keys") List<String> userKeyFilter) {
        return Flowable.create(obs -> {
            String name = UUID.randomUUID().toString();
            Map<String, Object> m = new HashMap<>();
            m.put("emitter", obs);

            Predicate<Event> p = event -> {
                List<String> user = (List<String>)event.getProperty("user");
                return user == null || userKeyFilter == null || userKeyFilter.contains(user.get(0));
            };
            m.put("predicate", p);

            listeners.put(name, m);
            logger.debug("Registered publication job listener {}", name);
            obs.setCancellable(() -> {
                logger.debug("Unregistered publication job listener {}", name);
                listeners.remove(name);
            });
        }, BackpressureStrategy.BUFFER);
    }

    public static void notifyListeners(Event event) {
        logger.debug("Notifying listeners for event {}, number of listeners: {}", event.getTopic(), listeners.size());
        if (!listeners.isEmpty()) {
            String eventTopic = event.getTopic();
            logger.debug("Received event on topic {}", eventTopic);
            GqlPublicationEvent gqlPublicationEvent;
            if (eventTopic.endsWith("/publication/done")) {
                gqlPublicationEvent = new GqlPublicationEvent(
                        GqlPublicationEvent.State.FINISHED,
                        (List<String>) event.getProperty("siteKey"),
                        (List<String>) event.getProperty("language"),
                        (List<String>) event.getProperty("paths"),
                        (List<String>) event.getProperty("user"));
            }
            else if (eventTopic.endsWith("/publication/unpublished")) {
                gqlPublicationEvent = new GqlPublicationEvent(
                        GqlPublicationEvent.State.UNPUBLISHED,
                        (List<String>) event.getProperty("siteKey"),
                        (List<String>) event.getProperty("language"),
                        (List<String>) event.getProperty("paths"),
                        (List<String>) event.getProperty("user"));
            }
            else {
                gqlPublicationEvent = new GqlPublicationEvent(
                        GqlPublicationEvent.State.STARTED,
                        (List<String>) event.getProperty("siteKey"),
                        (List<String>) event.getProperty("language"),
                        (List<String>) event.getProperty("paths"),
                        (List<String>) event.getProperty("user"));
            }

            listeners.forEach((name, m) -> {
                Predicate<Event> p = (Predicate<Event>) m.get("predicate");

                if (p.test(event)) {
                    FlowableEmitter<GqlPublicationEvent> emitter = (FlowableEmitter<GqlPublicationEvent>) m.get("emitter");
                    emitter.onNext(gqlPublicationEvent);
                }
            });
        }
    }
}
