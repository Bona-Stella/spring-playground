package com.github.stella.springredisjob.domain.post;

import com.github.stella.springredisjob.domain.post.dto.CreatePostRequest;
import com.github.stella.springredisjob.pubsub.NotifyPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PostEventSubscriber {
    private static final Logger log = LoggerFactory.getLogger(PostEventSubscriber.class);

    private final PostService postService;
    private final NotifyPublisher notifyPublisher;

    public PostEventSubscriber(PostService postService, NotifyPublisher notifyPublisher) {
        this.postService = postService;
        this.notifyPublisher = notifyPublisher;
    }

    // Redis MessageListenerAdapter에 의해 호출됨
    public void handleMessage(String message, String channel) {
        // 매우 단순한 파싱: "CREATE|title|content"
        try {
            String[] parts = message.split("\\|", 3);
            String type = parts.length > 0 ? parts[0] : "";
            if ("CREATE".equalsIgnoreCase(type) && parts.length == 3) {
                String title = parts[1];
                String content = parts[2];
                postService.create(new CreatePostRequest(title, content));
                String note = String.format("[POST][MQ] created via event title=%s", title);
                log.info(note);
                notifyPublisher.publish("notify", note);
            } else {
                log.warn("[POST][MQ] unsupported payload: {}", message);
            }
        } catch (Exception e) {
            log.warn("[POST][MQ] failed to process message={} err={}", message, e.getMessage());
        }
    }
}
