package com.github.stella.springredisjob.pubsub;

import com.github.stella.springredisjob.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pubsub")
public class PubSubController {
    private final NotifyPublisher publisher;

    public PubSubController(NotifyPublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping("/publish")
    public ResponseEntity<ApiResponse<String>> publish(
            @RequestParam(defaultValue = "notify") String channel,
            @RequestParam String message,
            HttpServletRequest request
    ) {
        publisher.publish(channel, message);
        return ResponseEntity.ok(ApiResponse.success("published", request.getRequestURI()));
    }
}
