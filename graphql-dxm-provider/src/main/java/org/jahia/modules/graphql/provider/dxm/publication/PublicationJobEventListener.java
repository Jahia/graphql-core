package org.jahia.modules.graphql.provider.dxm.publication;

import org.jahia.api.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = EventHandler.class, immediate = true, property = {
        org.osgi.framework.Constants.SERVICE_DESCRIPTION + "= Publication Job event handler",
        EventConstants.EVENT_TOPIC + "=" + Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/done",
        EventConstants.EVENT_TOPIC + "=" + Constants.CLUSTER_BROADCAST_TOPIC_PREFIX + "/publication/start",
})
public class PublicationJobEventListener implements EventHandler {

    static Logger logger = LoggerFactory.getLogger(PublicationJobEventListener.class);

    @Override
    public void handleEvent(Event event) {
        PublicationJobSubscriptionExtension.notifyListeners(event);
    }

}
