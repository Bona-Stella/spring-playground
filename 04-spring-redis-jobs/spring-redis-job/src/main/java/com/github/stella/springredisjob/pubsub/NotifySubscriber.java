package com.github.stella.springredisjob.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotifySubscriber {
    private static final Logger log = LoggerFactory.getLogger(NotifySubscriber.class);

    public void handleMessage(String message, String channel) {
        log.info("[Pub/Sub] Received on {}: {}", channel, message);
    }
}
