package com.github.stella.springmsamq.worker.web;

import com.github.stella.springmsamq.common.ApiResponse;
import com.github.stella.springmsamq.worker.config.WorkerProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/worker/dlq")
@RequiredArgsConstructor
public class DlqReprocessController {

    private final DlqService dlqService;
    private final WorkerProperties props;

    /**
     * DLQ 메시지 재처리 API
     */
    @PostMapping(value = "/reprocess", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> reprocess(
            @RequestParam(name = "token") String token,
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
            @RequestParam(name = "batchSize", required = false) Integer batchSize,
            @RequestParam(name = "target", defaultValue = "retry") String target,
            HttpServletRequest request
    ) {
        // 1. 보안 토큰 검증
        if (token == null || !token.equals(props.getReprocess().getToken())) {
            return ApiResponse.of(401, "UNAUTHORIZED", "Invalid token", null, request.getRequestURI());
        }

        // 2. 서비스 위임
        Map<String, Object> result = dlqService.processDlqMessages(dryRun, batchSize, target);

        return ApiResponse.success(result, request.getRequestURI());
    }
}