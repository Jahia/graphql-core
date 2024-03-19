package org.jahia.modules.graphql.provider.dxm.publication;

import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.jahia.api.Constants;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = {EventHandler.class,RecordConsumer.class}, immediate = true, property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "= Publication Job event handler",
        EventConstants.EVENT_TOPIC + "=" + Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/done",
        EventConstants.EVENT_TOPIC + "=" + Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/start",
        EventConstants.EVENT_TOPIC + "=" + Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/unpublished",
})
public class PublicationJobEventListener implements EventHandler, RecordConsumer {

    static Logger logger = LoggerFactory.getLogger(PublicationJobEventListener.class);

    private ClusterNode clusterNode;

    private Map<Long, Event> eventForRevision = new ConcurrentHashMap<>();

    @Activate
    public void activate() {

        try {
            SpringJackrabbitRepository jackrabbitRepository = (SpringJackrabbitRepository) org.jahia.services.SpringContextSingleton.getBean("jackrabbit");
            clusterNode = jackrabbitRepository.getClusterNode();

            if (clusterNode != null) {
                clusterNode.getJournal().register(this);
            }
        } catch (Exception e) {
            logger.warn("Failed to register record consumer. Publication events may be triggered before data is processed.", e);
        }
    }

    @Deactivate
    public void deactivate() {
        eventForRevision.clear();

        if (clusterNode != null) {
            clusterNode.getJournal().unregister(this);
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("Received event on topic {}", event.getTopic());
        List<String> revisions = (List<String>) event.getProperty("revision");

        if (revisions != null && clusterNode != null && Long.parseLong(revisions.get(0)) > clusterNode.getRevision()) {
            eventForRevision.put(Long.parseLong(revisions.get(0)), event);
        } else {
            PublicationJobSubscriptionExtension.notifyListeners(event);
        }
    }

    @Override
    public String getId() {
        return PublicationJobEventListener.class.getName();
    }

    @Override
    public long getRevision() {
        return clusterNode.getRevision();
    }

    @Override
    public void consume(Record record) {
        // Do nothing, not invoked
    }

    @Override
    public void setRevision(long l) {
        if (eventForRevision.containsKey(l)) {
            PublicationJobSubscriptionExtension.notifyListeners(eventForRevision.get(l));
            eventForRevision.remove(l);
        }
    }
}
