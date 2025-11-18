package com.github.stella.springredisjob.external.web;

import com.github.stella.springredisjob.common.api.ApiResponse;
import com.github.stella.springredisjob.external.mock.ExternalService;
import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import com.github.stella.springredisjob.external.mq.ExternalRequestPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external/mock")
public class ExternalController {
    private final ExternalService externalService;
    private final ExternalRequestPublisher requestPublisher;

    public ExternalController(ExternalService externalService, ExternalRequestPublisher requestPublisher) {
        this.externalService = externalService;
        this.requestPublisher = requestPublisher;
    }

    // 동기 호출: 정책 + 캐시 적용
    @GetMapping
    public ResponseEntity<ApiResponse<ExternalWeatherResponse>> get(
            @RequestParam String city,
            HttpServletRequest request
    ) {
        ExternalWeatherResponse res = externalService.getWeatherCached(city);
        return ResponseEntity.ok(ApiResponse.success(res, request.getRequestURI()));
    }

    // RAW 호출: 정책/캐시 우회
    @GetMapping("/raw")
    public ResponseEntity<ApiResponse<ExternalWeatherResponse>> raw(
            @RequestParam String city,
            HttpServletRequest request
    ) throws Exception {
        ExternalWeatherResponse res = externalService.fetchRaw(city);
        return ResponseEntity.ok(ApiResponse.success(res, request.getRequestURI()));
    }

    // 비동기 호출: 접수 후 즉시 응답
    @GetMapping("/async")
    public ResponseEntity<ApiResponse<String>> async(
            @RequestParam String city,
            HttpServletRequest request
    ) {
        externalService.fetchAsync(city);
        return ResponseEntity.ok(ApiResponse.success("accepted", request.getRequestURI()));
    }

    // MQ(=Redis Pub/Sub) 생산자: 요청을 큐에 적재
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<String>> queue(
            @RequestParam String city,
            HttpServletRequest request
    ) {
        requestPublisher.publish(city);
        return ResponseEntity.ok(ApiResponse.success("queued", request.getRequestURI()));
    }
}
