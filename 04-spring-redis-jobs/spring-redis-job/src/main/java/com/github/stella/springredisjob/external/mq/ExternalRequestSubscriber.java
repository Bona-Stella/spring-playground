package com.github.stella.springredisjob.external.mq;

import com.github.stella.springredisjob.external.mock.ExternalService;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import com.github.stella.springredisjob.pubsub.NotifyPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExternalRequestSubscriber {
    private static final Logger log = LoggerFactory.getLogger(ExternalRequestSubscriber.class);

    private final ExternalService externalService;
    private final NotifyPublisher notifyPublisher;

    public ExternalRequestSubscriber(ExternalService externalService, NotifyPublisher notifyPublisher) {
        this.externalService = externalService;
        this.notifyPublisher = notifyPublisher;
    }

    // Redis MessageListenerAdapter에 의해 호출됨
    public void handleMessage(String message, String channel) {
        String city = message;
        try {
            ExternalWeatherResponse res = externalService.getWeatherCached(city);
            String note = String.format("[MQ] fetched city=%s temp=%.1f desc=%s", res.city(), res.temperatureC(), res.description());
            log.info(note);
            // 완료 알림을 기존 notify 채널로 발행
            notifyPublisher.publish("notify", note);
        } catch (Exception e) {
            log.warn("[MQ] failed to fetch city={} error={}", city, e.getMessage());
        }
    }
}
